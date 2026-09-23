package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;

public record TeamGamesStatsDto(
        String teamId,
        String teamName,
        Integer days,
        int minimumPlayers,
        int rosterSize,
        StatsState state,
        TeamRecordDto overall,
        List<TeamRecordDto> byQueue,
        List<TeamRecordDto> bySide,
        List<TeamRecordDto> byPatch,
        List<TeamMemberPresenceDto> presence,
        List<TeamGameDto> games,
        long totalGames,
        long undecidedGames,
        boolean truncated,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        String viewerMemberId,
        Instant generatedAt
) {
}
