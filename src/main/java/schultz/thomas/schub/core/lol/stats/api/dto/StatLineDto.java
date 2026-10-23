package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record StatLineDto(
        String key,
        @Nullable String label,
        @Nullable String iconUrl,
        long games,
        long wins,
        @Nullable Double winRate,
        @Nullable Double kda,
        @Nullable Double killsPerGame,
        @Nullable Double deathsPerGame,
        @Nullable Double assistsPerGame,
        @Nullable Double csPerMinute,
        @Nullable Double goldPerMinute,
        @Nullable Double damagePerMinute,
        @Nullable Double damageTakenPerMinute,
        @Nullable Double visionPerMinute,
        @Nullable Double killParticipation,
        @Nullable Double deathShare,
        @Nullable Double wardsKilledPerMinute,
        @Nullable Double controlWardsPlaced,
        @Nullable Double damageShare,
        @Nullable Double deathsPer10,
        @Nullable Double timeDeadShare,
        @Nullable Double turretDamagePerMinute,
        @Nullable Double turretTakedowns,
        @Nullable Double epicMonsterDamagePerMinute,
        @Nullable Double platesDiff,
        @Schema(description = "Parties où les écarts à 15 min sont connus : celles dont on a la timeline.")
        long laningGames,
        @Nullable Double goldDiffAt15,
        @Nullable Double csDiffAt15,
        @Nullable Double xpDiffAt15,
        @Nullable Double killsDiffAt15,
        @Nullable Double killsAssistsAt15,
        @Nullable Double soloKills,
        long afkGames,
        long secondsPlayed,
        @Nullable Instant firstPlayedAt,
        @Nullable Instant lastPlayedAt,
        @Nullable StatComparisonDto versusRest
) {
}
