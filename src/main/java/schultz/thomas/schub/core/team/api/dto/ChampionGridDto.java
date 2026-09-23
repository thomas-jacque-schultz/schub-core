package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Moyennes des joueurs du champion dans un groupe de paliers. Métrique absente : pas assez de joueurs.
public record ChampionGridDto(int championId, String group, List<String> patches, Instant computedAt,
                              List<Double> percentiles, Map<String, Metric> metrics) {

    public record Metric(String polarity, long count, List<Double> values) {
    }
}
