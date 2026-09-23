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

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionEvaluator {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final List<ScopedAuthorityProvider> scopedAuthorityProviders;

    public boolean can(String actorId, Permission permission, ResourceRef resource) {
        return userRepository.findById(actorId)
                .map(actor -> can(actor, permission, resource))
                .orElse(false);
    }

    public boolean can(User actor, Permission permission, ResourceRef resource) {
        return effectivePermissions(actor, resource).contains(permission);
    }

    public void require(User actor, Permission permission, ResourceRef resource) {
        if (!can(actor, permission, resource)) {
            log.warn("Refus : {} n'a pas {} sur {}",
                    actor != null ? actor.getDiscordId() : "(acteur absent)", permission, resource);
            throw new AccessDeniedException("Permission " + permission + " requise");
        }
    }

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

    // resource null = question globale : ne jamais répondre oui parce que l'acteur est membre d'une équipe quelconque.
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
