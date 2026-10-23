package schultz.thomas.schub.core.lol.riot.business.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Tel que servi par le connecteur : la note se calcule dans le front, en situant une valeur dans ces grilles.
public record ReferenceGrid(
        List<String> patches,
        String scope,
        String position,
        Instant computedAt,
        List<Double> percentiles,
        Map<String, Metric> metrics,
        // Kills, morts et KDA d'une partie au poste, tous paliers confondus (Schub#129) ; absente avant leur calcul.
        Map<String, Distribution> distributions
) {

    public record Distribution(double step, long count, List<Long> counts) {
    }

    // rankMeans : moyenne par partie de chaque palier, du plus bas au plus haut ; absente si la métrique ne suit pas le rang.
    // allTiersMean : la moyenne par partie tous paliers confondus (Schub#146).
    public record Metric(String polarity, Map<String, Tier> tiers, Map<String, Double> rankMeans,
                         Double allTiersMean, List<String> missingTiers) {
    }

    public record Tier(long count, List<Double> values) {
    }
}
