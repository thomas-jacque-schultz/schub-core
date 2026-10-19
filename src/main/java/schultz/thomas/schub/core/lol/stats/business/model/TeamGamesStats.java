package schultz.thomas.schub.core.lol.stats.business.model;

import java.time.Instant;
import java.util.List;

public record TeamGamesStats(
        String teamId,
        String teamName,
        Integer days,
        int minimumPlayers,
        int rosterSize,
        StatsState state,
        TeamRecord overall,
        List<TeamRecord> byQueue,
        List<TeamRecord> bySide,
        List<TeamRecord> byPatch,
        List<TeamMemberPresence> presence,
        List<TeamGame> games,
        long totalGames,
        long undecidedGames,
        boolean truncated,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        String viewerMemberId,
        Instant generatedAt
) {
}
