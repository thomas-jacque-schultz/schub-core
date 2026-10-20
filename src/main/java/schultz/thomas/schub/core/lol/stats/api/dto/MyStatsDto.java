package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record MyStatsDto(
        @Nullable String displayName,
        @Nullable String riotGameName,
        @Nullable String riotTagLine,
        @Nullable Integer days,
        StatsState state,
        @Nullable StatsCoverageDto coverage,
        @Nullable RiotIngestProgressDto ingest,
        @Nullable StatLineDto overall,
        List<StatLineDto> champions,
        List<StatLineDto> positions,
        List<StatLineDto> queues,
        List<StatLineDto> months,
        List<StatLineDto> patches,
        List<RankedStandingDto> rankings,
        @Nullable RadarReferencesDto references,
        Instant generatedAt
) {
}
