package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Permission;

import java.util.Set;

/**
 * Ce que le BFF reçoit à la connexion : qui est l'utilisateur, et ce qu'il a le droit de faire.
 *
 * <p>Les permissions sont celles du <strong>rôle seul</strong>, sans portée de ressource : ce
 * sont elles qui voyagent dans le jeton. Les droits qu'un utilisateur tient d'être administrateur
 * d'un serveur ne s'y trouvent pas — ils dépendent du serveur visé, et c'est le cœur qui les
 * évalue au point d'action.</p>
 */
public record UserIdentityDto(
        UserDto user,
        Set<Permission> permissions
) {
}
