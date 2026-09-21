package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * @param days fenêtre demandée en jours ; nul = tout l'historique connu, lui-même borné par ce
 *             que Riot conserve (environ mille parties par joueur).
 */
public record TeamPlayersStatsDto(
        String teamId,
        String teamName,
        Integer days,
        int championsPerPlayer,
        List<PlayerStatsDto> players,
        String viewerMemberId,
        Instant generatedAt
) {
}
