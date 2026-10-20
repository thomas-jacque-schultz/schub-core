package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record TeamGameDto(
        String matchId,
        @Nullable Instant startedAt,
        long durationSeconds,
        int queueId,
        @Nullable String queue,
        @Nullable String patch,
        int presentPlayers,
        boolean splitSides,
        @Nullable Boolean win,
        List<TeamGamePlayerDto> players,
        List<TeamGamePlayerDto> allies,
        List<TeamGamePlayerDto> enemies,
        @Nullable SideRanksDto allyRanks,
        @Nullable SideRanksDto enemyRanks,
        @Nullable Instant ranksObservedAt
) {
}
