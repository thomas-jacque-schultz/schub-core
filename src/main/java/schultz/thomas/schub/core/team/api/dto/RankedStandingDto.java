package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

public record RankedStandingDto(
        String queue,
        String tier,
        String division,
        int leaguePoints,
        int wins,
        int losses,
        boolean hotStreak,
        Instant observedAt
) {
}
