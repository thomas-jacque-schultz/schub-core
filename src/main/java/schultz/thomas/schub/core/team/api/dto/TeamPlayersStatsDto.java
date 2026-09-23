package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record TeamPlayersStatsDto(
        String teamId,
        String teamName,
        Integer days,
        int championsPerPlayer,
        List<PlayerStatsDto> players,
        String viewerMemberId,
        Instant generatedAt
) {
}
