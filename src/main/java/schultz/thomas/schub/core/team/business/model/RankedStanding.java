package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;

public record RankedStanding(
        String queue,
        String riotQueueType,
        String tier,
        String division,
        int leaguePoints,
        int wins,
        int losses,
        boolean hotStreak,
        boolean inactive,
        Instant observedAt
) {
}
