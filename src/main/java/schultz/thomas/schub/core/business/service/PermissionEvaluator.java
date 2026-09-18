package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * « Cet acteur a-t-il le droit de faire X sur Y ? » — <strong>un seul endroit</strong>.
 *
 * <pre>
 *   autorité = permissions du rôle  ∪  (acteur ∈ serveur.admins → START/STOP sur CE serveur)
 * </pre>
 *
 * <p>Pourquoi ici et pas dans le BFF : le BFF ne connaît pas {@code GameServer.admins}, qui vit
 * dans le cœur. Une autorisation à cheval sur deux services serait une autorisation qu'on ne
 * peut pas relire (plan §1). Le BFF garde le contrôle <em>grossier</em> à partir des permissions
 * portées par le jeton ; le contrôle <em>fin</em> est ici, au point d'action.</p>
 *
 * <p>La ressource est volontairement générique : le chantier D posera exactement la même
 * question pour l'appartenance à une équipe, et réutilisera cette classe sans la rouvrir.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionEvaluator {

    /**
     * Ce qu'un administrateur de serveur gagne sur <em>son</em> serveur, et rien de plus
     * (décision n°11 du 18-09). Ni l'édition de la fiche, ni les ports : modifier les ports,
     * c'est écrire dans la table de redirections de la box.
     */
    private static final Set<Permission> SERVER_ADMIN_SCOPED =
            Set.of(Permission.SERVER_START, Permission.SERVER_STOP);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GameServerService gameServerService;

    /** La question du plan §A.1, telle quelle. L'acteur est désigné par son <em>id interne</em>. */
    public boolean can(String actorId, Permission permission, ResourceRef resource) {
        return userRepository.findById(actorId)
                .map(actor -> can(actor, permission, resource))
                .orElse(false);
    }

    public boolean can(User actor, Permission permission, ResourceRef resource) {
        return effectivePermissions(actor, resource).contains(permission);
    }

    /** Refuse en 403 plutôt que de renvoyer un booléen que l'appelant pourrait oublier de lire. */
    public void require(User actor, Permission permission, ResourceRef resource) {
        if (!can(actor, permission, resource)) {
            log.warn("Refus : {} n'a pas {} sur {}",
                    actor != null ? actor.getDiscordId() : "(acteur absent)", permission, resource);
            throw new AccessDeniedException("Permission " + permission + " requise");
        }
    }

    /** Les permissions du rôle seul — ce que porte le jeton du BFF, sans portée de ressource. */
    public Set<Permission> rolePermissions(User actor) {
        if (actor == null || actor.getRoleId() == null) {
            return EnumSet.noneOf(Permission.class);
        }
        return roleRepository.findById(actor.getRoleId())
                .map(Role::getPermissions)
                .map(permissions -> permissions.isEmpty()
                        ? EnumSet.noneOf(Permission.class)
                        : EnumSet.copyOf(permissions))
                .orElseGet(() -> {
                    log.warn("L'utilisateur {} porte un rôle inconnu ({}) — traité comme sans droits",
                            actor.getDiscordId(), actor.getRoleId());
                    return EnumSet.noneOf(Permission.class);
                });
    }

    /**
     * L'union des deux sources d'autorité pour une ressource donnée.
     *
     * <p>{@code resource} à {@code null} pose la question globale : « a-t-il le droit de créer
     * un serveur ? » n'a pas de ressource, et ne doit surtout pas répondre oui parce qu'il est
     * admin d'un serveur quelconque.</p>
     */
    public Set<Permission> effectivePermissions(User actor, ResourceRef resource) {
        Set<Permission> effective = rolePermissions(actor);
        if (actor == null || resource == null) {
            return effective;
        }
        if (resource.type() == ResourceType.GAME_SERVER && isServerAdmin(actor, resource.id())) {
            effective.addAll(SERVER_ADMIN_SCOPED);
        }
        return effective;
    }

    private boolean isServerAdmin(User actor, String slug) {
        Optional<GameServer> server = gameServerService.findBySlug(slug);
        return server.isPresent()
                && server.get().getAdmins() != null
                && server.get().getAdmins().contains(actor.getId());
    }
}
