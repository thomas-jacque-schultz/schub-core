package schultz.thomas.schub.core.lol.stats.business.model;

import java.time.Instant;

public record StatsCoverage(
        long games,
        long knownMatches,
        long pendingMatches,
        boolean tracked,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        Instant lastSyncAt
) {
}
