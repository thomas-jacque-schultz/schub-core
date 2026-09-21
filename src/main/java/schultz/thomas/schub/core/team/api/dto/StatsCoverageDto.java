package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

/**
 * Sur quoi portent les chiffres qui l'accompagnent. Sans ça, un taux calculé sur trois parties
 * se lit comme un taux calculé sur trois cents.
 *
 * @param tracked         faux avec des parties quand même : ce compte n'est connu que pour avoir
 *                        été croisé dans l'historique d'un autre. Les chiffres sont vrais mais
 *                        partiels.
 * @param pendingMatches  parties repérées dont le détail n'est pas encore collecté.
 */
public record StatsCoverageDto(
        long games,
        long knownMatches,
        long pendingMatches,
        boolean tracked,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        Instant lastSyncAt
) {
}
