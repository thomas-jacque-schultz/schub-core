package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;
import java.util.List;

public record MyGames(
        Integer days,
        StatsState state,
        List<TeamGame> games,
        long totalGames,
        boolean truncated,
        String viewerMemberId,
        Instant generatedAt
) {
}
