package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

/**
 * Où en est la collecte des parties de ce compte, vue du connecteur.
 *
 * <p><strong>Nul quand le connecteur n'a pas répondu</strong>, et c'est un état légitime, pas un
 * échec : le profil s'affiche sans lui. Le connecteur Riot peut être éteint ou sans clé — celle
 * de développement expire toutes les 24 h — et faire échouer {@code GET /me} pour cette raison
 * rendrait le site inutilisable pour tout le monde.</p>
 *
 * @param estimatedReadyAt quand la file aura fini ce qui concerne ce joueur, en tenant compte de
 *                         ce qui passe devant. {@code null} s'il n'y a rien en attente.
 */
public record RiotIngestDto(
        long pending,
        long running,
        Instant estimatedReadyAt
) {
}
