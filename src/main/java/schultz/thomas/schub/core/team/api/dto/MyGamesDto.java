package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;

public record MyGamesDto(
        Integer days,
        StatsState state,
        List<TeamGameDto> games,
        long totalGames,
        boolean truncated,
        String viewerMemberId,
        Instant generatedAt
) {
}
