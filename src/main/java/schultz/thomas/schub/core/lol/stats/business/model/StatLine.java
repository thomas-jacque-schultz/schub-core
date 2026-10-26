package schultz.thomas.schub.core.lol.stats.business.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StatLine(
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
        @Schema(description = "Kills et assists à 15 min, par partie à timeline (Schub#146).")
        Double killsAssistsAt15,
        @Schema(description = "Solo kills par partie, sur celles où Riot les sert (Schub#146).")
        Double soloKills,
        @Schema(description = "Au support, parties où la part d'images avec l'ADC avant 15 min est connue (Schub#153).")
        long supportEarlyGames,
        @Schema(description = "Part moyenne (0 à 1) des images avant 15 min passées avec l'ADC.")
        Double adcEarlyShare,
        @Schema(description = "Départs d'auprès de l'ADC avant 15 min, par partie.")
        Double roams,
        @Schema(description = "Au support, parties où la part d'images avec le jungler après 15 min est connue.")
        long supportLateGames,
        @Schema(description = "Part moyenne (0 à 1) des images après 15 min passées avec le jungler.")
        Double junglerLateShare,
        long afkGames,
        long secondsPlayed,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        StatComparison versusRest
) {
}
