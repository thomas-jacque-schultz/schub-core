package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;

/**
 * Les statistiques du lecteur.
 *
 * <p>Servie par {@code GET /me/stats}, sans paramètre d'identité : le sujet est l'acteur du
 * jeton. Il n'existe pas de chemin portant un puuid, et c'est volontaire — il suffirait d'un
 * puuid croisé ailleurs pour sonder l'historique de n'importe qui.</p>
 */
public record MyStatsDto(
        String displayName,
        String riotGameName,
        String riotTagLine,
        Integer days,
        StatsState state,
        StatsCoverageDto coverage,
        RiotIngestProgressDto ingest,
        StatLineDto overall,
        List<StatLineDto> champions,
        List<StatLineDto> positions,
        List<StatLineDto> queues,
        List<StatLineDto> months,
        List<RankedStandingDto> rankings,
        Instant generatedAt
) {
}
