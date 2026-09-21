package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;

/**
 * Le panneau « équipe ».
 *
 * @param minimumPlayers seuil retenu pour qu'une partie soit une partie d'équipe. Affiché, parce
 *                       qu'il définit tout le reste du panneau.
 * @param truncated      il existe des parties plus anciennes au-dessus du seuil que la limite a
 *                       coupées : les totaux portent sur ce qui est rendu.
 */
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
