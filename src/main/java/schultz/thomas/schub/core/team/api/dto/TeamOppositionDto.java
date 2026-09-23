package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;

public record TeamOppositionDto(
        String teamId,
        Integer days,
        StatsState state,
        int games,
        int gamesWithRanks,
        Double medianLagDays,
        List<TeamRecordDto> byEnemyTier,
        List<TeamRecordDto> byGap,
        List<PositionOppositionDto> byPosition,
        String ceilingTier,
        int ceilingMinimumGames,
        TeamEarlyGameDto early,
        TeamLevelDto level,
        Instant generatedAt
) {
}
