package schultz.thomas.schub.core.api.dto;

import java.time.Duration;

/**
 * Ce qu'un changement de compte Riot emporte — <strong>servi avant, pour avertir, et après,
 * pour confirmer</strong>.
 *
 * <p>Changer de compte n'est pas corriger une faute de frappe : le {@code puuid} est la clé de
 * tout le domaine de jeu, et en changer détache l'historique déjà collecté. L'écran ne peut le
 * dire que si l'API le dit ; d'où ce corps, rendu en 409 tant que {@code confirmChange} n'est
 * pas posé, puis sur la réponse du changement effectué.</p>
 *
 * @param statsReset         les participations déjà collectées restent attachées à l'ancien
 *                           {@code puuid} : les statistiques personnelles repartent de zéro.
 *                           Rien n'est effacé — l'ancien compte garde son historique, il cesse
 *                           seulement d'être le vôtre.
 * @param ingestRestarted    une collecte est demandée pour le nouveau {@code puuid}. Fausse si
 *                           le connecteur n'a pas répondu, ou si le Riot ID n'a pas été résolu :
 *                           il n'y a alors rien à collecter tant qu'il ne l'est pas.
 * @param estimatedMatches   ce que Riot garde par joueur, mesuré : environ mille parties.
 * @param estimatedDuration  au débit autorisé par la clé courante, un appel par partie.
 * @param rosterSlotsToClaim les places d'effectif rattachées à l'ancien {@code puuid} ne suivent
 *                           pas d'elles-mêmes : {@code POST /teams/claim} les resynchronise, et
 *                           c'est au front de l'appeler après un changement accepté. Le domaine
 *                           de l'identité n'écrit pas dans celui des équipes (plan §D.2).
 */
public record RiotAccountChangeDto(
        String previousRiotId,
        String riotId,
        boolean statsReset,
        boolean ingestRestarted,
        int estimatedMatches,
        Duration estimatedDuration,
        boolean rosterSlotsToClaim
) {
}
