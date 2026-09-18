package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Permission;

import java.util.Set;

/**
 * Un rôle tel que le cœur l'expose.
 *
 * @param system un rôle système ne se supprime pas et ne se renomme pas ; l'interface s'en sert
 *               pour griser les boutons plutôt que de laisser l'utilisateur découvrir le refus
 */
public record RoleDto(
        String id,
        String name,
        Set<Permission> permissions,
        boolean system
) {
}
