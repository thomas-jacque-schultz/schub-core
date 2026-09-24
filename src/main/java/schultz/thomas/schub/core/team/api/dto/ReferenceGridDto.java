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
        List<Double> percentiles,
        Map<String, Metric> metrics
) {

    // rankMedians : médiane par partie de chaque palier, du plus bas au plus haut ; absente si la métrique ne suit pas le rang.
    public record Metric(String polarity, Map<String, Tier> tiers, Map<String, Double> rankMedians,
                         List<String> missingTiers) {
    }

    public record Tier(long count, List<Double> values) {
    }
}
