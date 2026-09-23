package schultz.thomas.schub.core.team.api.dto;

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
        long afkGames,
        long secondsPlayed,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        StatComparisonDto versusRest
) {
}
