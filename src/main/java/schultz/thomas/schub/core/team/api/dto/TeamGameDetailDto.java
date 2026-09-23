package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record TeamGameDetailDto(
        String teamId,
        TeamGameDto game,
        List<MatchupDto> matchups,
        boolean timelineAvailable,
        Instant ranksObservedAt,
        EarlyGameDto early,
        String viewerMemberId
) {
}
