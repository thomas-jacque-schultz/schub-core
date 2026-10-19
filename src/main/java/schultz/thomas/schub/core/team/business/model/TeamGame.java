package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;
import java.util.List;

public record TeamGame(
        String matchId,
        Instant startedAt,
        long durationSeconds,
        int queueId,
        String queue,
        String patch,
        int presentPlayers,
        boolean splitSides,
        Boolean win,
        List<TeamGamePlayer> players,
        List<TeamGamePlayer> allies,
        List<TeamGamePlayer> enemies,
        SideRanks allyRanks,
        SideRanks enemyRanks,
        Instant ranksObservedAt
) {
}
