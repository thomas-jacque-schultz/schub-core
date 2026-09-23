package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.MeDto;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionEvaluator permissionEvaluator;
    private final RiotAccountService riotAccountService;

    private static final int DISPLAY_NAME_MAX = 32;

    public Optional<User> findByDiscordId(String discordId) {
        return discordId == null || discordId.isBlank()
                ? Optional.empty()
                : userRepository.findByDiscordId(discordId);
    }

    public Optional<User> findByRiotPuuid(String riotPuuid) {
        return riotPuuid == null || riotPuuid.isBlank()
                ? Optional.empty()
                : userRepository.findByRiotPuuid(riotPuuid);
    }

    // En-tête absent = service Schub agissant pour son compte. En-tête présent mais inconnu = refus.
    public User requireActor(String actorDiscordId) {
        return findByDiscordId(actorDiscordId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Acteur inconnu ou absent — en-tête X-Actor-Id requis"));
    }

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


    public String displayNameOf(User user) {
        String choisi = user.getDisplayName();
        return choisi == null || choisi.isBlank() ? user.getDiscordUsername() : choisi;
    }

    public User changeDisplayName(User actor, String displayName) {
        String propre = displayName == null ? null : displayName.trim();
        if (propre == null || propre.isEmpty()) {
            actor.setDisplayName(null);
            log.info("Nom d'affichage de {} rendu au pseudo Discord", actor.getDiscordId());
            return userRepository.save(actor);
        }
        if (propre.length() > DISPLAY_NAME_MAX) {
            throw new IllegalArgumentException(
                    "Un nom d'affichage ne dépasse pas " + DISPLAY_NAME_MAX + " caractères");
        }
        if (propre.codePoints().noneMatch(Character::isLetterOrDigit)) {
            throw new IllegalArgumentException(
                    "Un nom d'affichage doit contenir au moins une lettre ou un chiffre");
        }
        actor.setDisplayName(propre);
        return userRepository.save(actor);
    }

    public MeDto toMeDto(User user) {
        Role role = user.getRoleId() == null ? null : roleRepository.findById(user.getRoleId()).orElse(null);
        return new MeDto(
                user.getId(),
                new MeDto.DiscordIdentityDto(user.getDiscordId(), user.getDiscordUsername(), user.getAvatarUrl()),
                displayNameOf(user),
                user.getDisplayName() != null && !user.getDisplayName().isBlank(),
                new MeDto.RoleSummaryDto(role == null ? null : role.getName(),
                        permissionEvaluator.rolePermissions(user)),
                riotAccountService.of(user));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Règle du sous-ensemble contre l'auto-élévation, plus : pas de modification de son propre rôle,
     * pas de retrait du dernier OWNER, ROLE_MANAGE et OWNER jamais attribuables.
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

    public UserIdentityDto toIdentityDto(User user) {
        return new UserIdentityDto(toDto(user), permissionEvaluator.rolePermissions(user));
    }

    public UserDto toDto(User user) {
        String roleName = user.getRoleId() == null ? null
                : roleRepository.findById(user.getRoleId()).map(Role::getName).orElse(null);
        return toDto(user, roleName);
    }

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
                displayNameOf(user),
                user.getAvatarUrl(),
                user.getRoleId(),
                roleName,
                user.getRiotGameName(),
                user.getRiotTagLine(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }

    public Map<String, User> byIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
