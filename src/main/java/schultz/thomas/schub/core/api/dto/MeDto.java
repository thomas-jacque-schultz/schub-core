package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Permission;

import java.util.Set;

/**
 * Tout ce qu'un compte connecté doit savoir de lui-même, en un appel.
 *
 * <p>Le front en avait besoin de trois — l'identité, les permissions, le compte Riot — pour
 * afficher un écran de profil. Trois appels, trois états de chargement et trois façons d'échouer
 * pour une seule page : {@code GET /me} les réunit, et c'est la seule route que l'écran de
 * profil ait à connaître.</p>
 *
 * <p><strong>Ce qui manque ne fait pas échouer l'appel.</strong> {@code riot.ingest} est nul
 * quand le connecteur Riot ne répond pas, et le profil s'affiche quand même — la disponibilité
 * d'une API tierce ne conditionne aucune fonctionnalité du domaine (plan §D, lot D).</p>
 *
 * @param displayName toujours rempli : le nom choisi, à défaut le pseudo Discord
 * @param role        les permissions du <em>rôle seul</em>, comme {@code UserIdentityDto} :
 *                    celles qu'un acteur tient d'être administrateur d'un serveur ou membre
 *                    d'une équipe dépendent de la ressource visée et s'évaluent au point d'action
 */
public record MeDto(
        String userId,
        DiscordIdentityDto discord,
        String displayName,
        RoleSummaryDto role,
        RiotAccountDto riot
) {

    public record DiscordIdentityDto(String id, String username, String avatarUrl) {
    }

    public record RoleSummaryDto(String name, Set<Permission> permissions) {
    }
}
