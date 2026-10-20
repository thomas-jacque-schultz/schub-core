package schultz.thomas.schub.core.lol.riot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
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
        Map<String, Metric> metrics,
        // Kills, morts et KDA d'une partie au poste, tous paliers confondus (Schub#129) : parties par case de largeur step.
        @Nullable Map<String, Distribution> distributions
) {

    @Schema(name = "ReferenceGridDistribution")
    public record Distribution(double step, long count, List<Long> counts) {
    }

    // rankMeans : moyenne par partie de chaque palier, du plus bas au plus haut ; absente si la métrique ne suit pas le rang.
    @Schema(name = "ReferenceGridMetric")
    public record Metric(String polarity, Map<String, Tier> tiers, @Nullable Map<String, Double> rankMeans,
                         List<String> missingTiers) {
    }

    public record Tier(long count, List<Double> values) {
    }
}
