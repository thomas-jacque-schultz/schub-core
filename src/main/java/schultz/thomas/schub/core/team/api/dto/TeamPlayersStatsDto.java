package schultz.thomas.schub.core.team.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record TeamPlayersStatsDto(
        String teamId,
        String teamName,
        Integer days,
        int championsPerPlayer,
        List<PlayerStatsDto> players,
        String viewerMemberId,
        Instant generatedAt,
        @Schema(description = "Parties d'équipe de la période (au moins premadeMinimum membres). Absente : inconnue.")
        Long premadeGames,
        int premadeMinimum
) {
}
