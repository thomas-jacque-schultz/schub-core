package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record StatsCoverageDto(
        long games,
        long knownMatches,
        long pendingMatches,
        boolean tracked,
        @Nullable Instant firstPlayedAt,
        @Nullable Instant lastPlayedAt,
        @Nullable Instant lastSyncAt
) {
}
