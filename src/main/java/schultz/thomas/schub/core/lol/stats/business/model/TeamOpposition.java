package schultz.thomas.schub.core.lol.stats.business.model;

import java.time.Instant;
import java.util.List;

public record TeamOpposition(
        String teamId,
        Integer days,
        StatsState state,
        int games,
        int gamesWithRanks,
        Double medianLagDays,
        List<TeamRecord> byEnemyTier,
        List<TeamRecord> byGap,
        List<PositionOpposition> byPosition,
        String ceilingTier,
        int ceilingMinimumGames,
        TeamEarlyGame early,
        TeamLevel level,
        Instant generatedAt
) {
}
