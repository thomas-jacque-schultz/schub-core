package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Tel que servi par le connecteur : la note se calcule dans le front, en situant une valeur dans ces grilles.
public record ReferenceGridDto(
        List<String> patches,
        String scope,
        String position,
        Instant computedAt,
        String distribution,
        List<Double> percentiles,
        List<Level> levels,
        Map<String, Metric> metrics
) {

    public record Level(String tier, double fromPercentile) {
    }

    public record Metric(String polarity, Map<String, Tier> tiers, List<Double> ladder, List<String> missingTiers) {
    }

    public record Tier(long count, List<Double> values) {
    }
}
