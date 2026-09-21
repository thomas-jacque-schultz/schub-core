package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * « Cet acteur a-t-il le droit de faire X sur Y ? » — <strong>un seul endroit</strong>.
 *
 * <pre>
 *   autorité = permissions du rôle  ∪  (ce que la ressource visée donne à qui lui appartient)
 * </pre>
 *
 * <p>Pourquoi ici et pas dans le BFF : le BFF ne connaît pas les appartenances, qui vivent dans le
 * cœur. Une autorisation à cheval sur deux services serait une autorisation qu'on ne peut pas
 * relire (plan §1). Le BFF garde le contrôle <em>grossier</em> à partir des permissions portées
 * par le jeton ; le contrôle <em>fin</em> est ici, au point d'action.</p>
 *
 * <p>Un serveur de jeu n'est plus une de ces ressources : piloter un serveur vient du rôle et de
 * lui seul. Une équipe, si — c'est l'appartenance qui donne les droits sur <em>cette</em> équipe.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionEvaluator {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Les domaines qui savent ce que <em>leur</em> ressource donne à qui lui appartient.
     *
     * <p>L'évaluateur n'a besoin d'aucun dépôt d'un domaine particulier, ce que l'interdit n°1 du
     * plan §D.2 exige : chacun déclare lui-même ce que sa ressource accorde.</p>
     */
    private final List<ScopedAuthorityProvider> scopedAuthorityProviders;

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
     * <p>{@code resource} à {@code null} pose la question globale : « a-t-il le droit de créer une
     * équipe ? » n'a pas de ressource, et ne doit surtout pas répondre oui parce qu'il est membre
     * d'une équipe quelconque.</p>
     */
    public Set<Permission> effectivePermissions(User actor, ResourceRef resource) {
        Set<Permission> effective = rolePermissions(actor);
        if (actor == null || resource == null) {
            return effective;
        }
        for (ScopedAuthorityProvider provider : scopedAuthorityProviders) {
            if (provider.resourceType() == resource.type()) {
                effective.addAll(provider.grantedTo(actor, resource.id()));
            }
        }
        return effective;
    }
}
