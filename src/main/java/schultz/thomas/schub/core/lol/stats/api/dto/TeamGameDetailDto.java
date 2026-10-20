package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record TeamGameDetailDto(
        @Nullable String teamId,
        TeamGameDto game,
        List<MatchupDto> matchups,
        boolean timelineAvailable,
        @Nullable Instant ranksObservedAt,
        @Nullable EarlyGameDto early,
        @Nullable String viewerMemberId,
        @Nullable Integer days,
        List<GamePlayerMetricsDto> metrics
) {
}
