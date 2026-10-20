package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record TeamGamesStatsDto(
        String teamId,
        String teamName,
        @Nullable Integer days,
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
        @Nullable Instant firstPlayedAt,
        @Nullable Instant lastPlayedAt,
        @Nullable String viewerMemberId,
        Instant generatedAt
) {
}
