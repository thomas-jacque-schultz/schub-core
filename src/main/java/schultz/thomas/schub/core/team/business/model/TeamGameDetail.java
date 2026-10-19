package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;
import java.util.List;

public record TeamGameDetail(
        String teamId,
        TeamGame game,
        List<Matchup> matchups,
        boolean timelineAvailable,
        Instant ranksObservedAt,
        EarlyGame early,
        String viewerMemberId,
        Integer days,
        List<GamePlayerMetrics> metrics
) {
}
