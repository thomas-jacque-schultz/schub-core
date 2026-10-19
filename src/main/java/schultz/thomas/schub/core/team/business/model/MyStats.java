package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;
import java.util.List;

public record MyStats(
        String displayName,
        String riotGameName,
        String riotTagLine,
        Integer days,
        StatsState state,
        StatsCoverage coverage,
        RiotIngestProgress ingest,
        StatLine overall,
        List<StatLine> champions,
        List<StatLine> positions,
        List<StatLine> queues,
        List<StatLine> months,
        List<StatLine> patches,
        List<RankedStanding> rankings,
        RadarReferences references,
        Instant generatedAt
) {
}
