package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

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
