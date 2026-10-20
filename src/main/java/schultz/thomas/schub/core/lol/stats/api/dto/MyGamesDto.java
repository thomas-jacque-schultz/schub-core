package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record MyGamesDto(
        @Nullable Integer days,
        StatsState state,
        List<TeamGameDto> games,
        long totalGames,
        boolean truncated,
        @Nullable String viewerMemberId,
        Instant generatedAt
) {
}
