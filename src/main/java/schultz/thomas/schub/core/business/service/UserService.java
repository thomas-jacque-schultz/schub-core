package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.UserDto;
import schultz.thomas.schub.core.api.dto.UserIdentityDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Les comptes : leur création à la première connexion, leur rôle, et les règles qui le bornent. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionEvaluator permissionEvaluator;

    public Optional<User> findByDiscordId(String discordId) {
        return discordId == null || discordId.isBlank()
                ? Optional.empty()
                : userRepository.findByDiscordId(discordId);
    }

    /**
     * L'acteur d'une requête, ou un refus.
     *
     * <p>Deux cas qu'il ne faut surtout pas confondre : <em>en-tête absent</em> — l'appelant est
     * un service Schub qui agit pour son propre compte, ce que le contrôleur traite au cas par
     * cas ; <em>en-tête présent mais inconnu</em> — quelqu'un affirme une identité qui n'existe
     * pas, et c'est un refus. Les confondre ferait d'un id inventé un laissez-passer.</p>
     */
    public User requireActor(String actorDiscordId) {
        return findByDiscordId(actorDiscordId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Acteur inconnu ou absent — en-tête X-Actor-Id requis"));
    }

    /**
     * Retrouve le compte, ou le crée au rôle {@code VISITEUR}.
     *
     * <p>C'est le point d'entrée de la connexion : tout le monde peut se connecter, sans
     * inscription préalable, et repart avec le rôle qui ne donne que la consultation
     * (décision n°10 du 18-09).</p>
     *
     * <p>Le pseudo et l'avatar sont rafraîchis à chaque passage quand l'appelant les fournit :
     * ils viennent de Discord, qui en est la source, et un pseudo figé au premier jour finirait
     * par désigner quelqu'un d'autre.</p>
     */
    public User findOrCreateByDiscordId(String discordId, String discordUsername, String avatarUrl) {
        if (discordId == null || discordId.isBlank()) {
            throw new IllegalArgumentException("Un identifiant Discord est obligatoire");
        }
        User user = userRepository.findByDiscordId(discordId).orElseGet(() -> {
            User created = new User();
            created.setDiscordId(discordId);
            created.setRoleId(requireRoleByName(SystemRole.VISITEUR.roleName()).getId());
            created.setCreatedAt(Instant.now());
            log.info("Nouveau compte créé pour l'identifiant Discord {} au rôle {}",
                    discordId, SystemRole.VISITEUR.roleName());
            return created;
        });

        if (discordUsername != null && !discordUsername.isBlank()) {
            user.setDiscordUsername(discordUsername);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Attribue un rôle — et c'est ici que se ferme le chemin d'élévation classique.
     *
     * <p>Sans la règle du sous-ensemble, un {@code ADMINISTRATOR} se fabrique un rôle « tout
     * coché », se l'attribue, et la hiérarchie n'existe plus. Elle tient en trois lignes, à
     * condition d'y penser en écrivant le modèle plutôt qu'après (plan §A.1).</p>
     *
     * <p>Trois refus supplémentaires, tous des garde-fous anti-verrouillage :</p>
     * <ul>
     *   <li>on ne modifie pas son propre rôle — se rétrograder soi-même est l'erreur la plus
     *       facile à commettre et la plus pénible à réparer ;</li>
     *   <li>on ne retire pas le dernier {@code OWNER} — sinon plus personne ne détient
     *       {@code ROLE_MANAGE}, et il n'y a aucun moyen de le rattraper depuis l'interface ;</li>
     *   <li>{@code ROLE_MANAGE} et le rôle {@code OWNER} ne sont jamais attribuables
     *       (décision n°2 du 18-09).</li>
     * </ul>
     */
    public User assignRole(User actor, String targetUserId, String roleId) {
        permissionEvaluator.require(actor, Permission.USER_ROLE_ASSIGN, null);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Aucun utilisateur d'identifiant '" + targetUserId + "'"));
        Role candidate = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Aucun rôle d'identifiant '" + roleId + "'"));

        if (actor.getId().equals(target.getId())) {
            throw new AccessDeniedException("On ne modifie pas son propre rôle");
        }
        if (candidate.getPermissions().contains(Permission.ROLE_MANAGE)
                || SystemRole.OWNER.roleName().equals(candidate.getName())) {
            throw new AccessDeniedException("Le rôle " + candidate.getName() + " n'est pas attribuable");
        }

        Set<Permission> actorPermissions = permissionEvaluator.rolePermissions(actor);
        if (!actorPermissions.containsAll(candidate.getPermissions())) {
            throw new AccessDeniedException(
                    "On n'attribue pas un rôle plus puissant que le sien : " + candidate.getName());
        }

        Role ownerRole = requireRoleByName(SystemRole.OWNER.roleName());
        if (ownerRole.getId().equals(target.getRoleId())
                && userRepository.countByRoleId(ownerRole.getId()) <= 1) {
            throw new AccessDeniedException("Le dernier " + SystemRole.OWNER.roleName() + " ne peut pas être rétrogradé");
        }

        target.setRoleId(candidate.getId());
        log.info("Rôle de {} changé en {} par {}", target.getDiscordId(), candidate.getName(), actor.getDiscordId());
        return userRepository.save(target);
    }

    public Role requireRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(
                        "Le rôle système '" + name + "' est absent de la base — la migration n'a pas tourné"));
    }

    // --- projections ---

    public UserIdentityDto toIdentityDto(User user) {
        return new UserIdentityDto(toDto(user), permissionEvaluator.rolePermissions(user));
    }

    public UserDto toDto(User user) {
        String roleName = user.getRoleId() == null ? null
                : roleRepository.findById(user.getRoleId()).map(Role::getName).orElse(null);
        return toDto(user, roleName);
    }

    /** Variante de lot : un seul aller-retour vers les rôles pour toute la liste. */
    public List<UserDto> toDtos(List<User> users) {
        Map<String, String> roleNames = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));
        return users.stream()
                .map(user -> toDto(user, roleNames.get(user.getRoleId())))
                .toList();
    }

    private UserDto toDto(User user, String roleName) {
        return new UserDto(
                user.getId(),
                user.getDiscordId(),
                user.getDiscordUsername(),
                user.getAvatarUrl(),
                user.getRoleId(),
                roleName,
                user.getRiotGameName(),
                user.getRiotTagLine(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }

    /** Les comptes désignés par leurs ids internes, indexés — pour résoudre les admins d'un serveur. */
    public Map<String, User> byIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
