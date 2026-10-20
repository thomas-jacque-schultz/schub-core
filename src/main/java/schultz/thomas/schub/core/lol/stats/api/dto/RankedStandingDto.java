package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record RankedStandingDto(
        @Nullable String queue,
        @Nullable String riotQueueType,
        @Nullable String tier,
        @Nullable String division,
        int leaguePoints,
        int wins,
        int losses,
        boolean hotStreak,
        boolean inactive,
        @Nullable Instant observedAt
) {
}
