package schultz.thomas.schub.core.lol.stats.business.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record TeamPlayersStats(
        String teamId,
        String teamName,
        Integer days,
        int championsPerPlayer,
        List<PlayerStats> players,
        String viewerMemberId,
        Instant generatedAt,
        @Schema(description = "Parties d'équipe de la période (au moins premadeMinimum membres). Absente : inconnue.")
        Long premadeGames,
        int premadeMinimum
) {
}
