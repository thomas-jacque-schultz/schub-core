package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;

import java.time.Instant;
import java.util.List;

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
        List<StatLineDto> patches,
        List<RankedStandingDto> rankings,
        RadarReferencesDto references,
        Instant generatedAt
) {
}
