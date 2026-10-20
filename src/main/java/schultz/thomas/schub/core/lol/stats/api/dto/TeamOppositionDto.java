package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record TeamOppositionDto(
        String teamId,
        @Nullable Integer days,
        StatsState state,
        int games,
        int gamesWithRanks,
        @Nullable Double medianLagDays,
        List<TeamRecordDto> byEnemyTier,
        List<TeamRecordDto> byGap,
        List<PositionOppositionDto> byPosition,
        @Nullable String ceilingTier,
        int ceilingMinimumGames,
        @Nullable TeamEarlyGameDto early,
        @Nullable TeamLevelDto level,
        Instant generatedAt
) {
}
