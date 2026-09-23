package schultz.thomas.schub.core.team.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StatLineDto(
        String key,
        String label,
        String iconUrl,
        long games,
        long wins,
        Double winRate,
        Double kda,
        Double killsPerGame,
        Double deathsPerGame,
        Double assistsPerGame,
        Double csPerMinute,
        Double goldPerMinute,
        Double damagePerMinute,
        Double damageTakenPerMinute,
        Double visionPerMinute,
        Double killParticipation,
        Double deathShare,
        Double wardsKilledPerMinute,
        Double controlWardsPlaced,
        Double damageShare,
        Double deathsPer10,
        Double timeDeadShare,
        Double turretDamagePerMinute,
        Double turretTakedowns,
        Double epicMonsterDamagePerMinute,
        Double platesDiff,
        @Schema(description = "Parties où les écarts à 15 min sont connus : celles dont on a la timeline.")
        long laningGames,
        Double goldDiffAt15,
        Double csDiffAt15,
        Double xpDiffAt15,
        Double killsDiffAt15,
        long afkGames,
        long secondsPlayed,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        StatComparisonDto versusRest
) {
}
