package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record TeamPlayersStatsDto(
        String teamId,
        String teamName,
        @Nullable Integer days,
        int championsPerPlayer,
        List<PlayerStatsDto> players,
        @Nullable String viewerMemberId,
        Instant generatedAt,
        @Schema(description = "Parties d'équipe de la période (au moins premadeMinimum membres). Absente : inconnue.")
        @Nullable Long premadeGames,
        int premadeMinimum
) {
}
