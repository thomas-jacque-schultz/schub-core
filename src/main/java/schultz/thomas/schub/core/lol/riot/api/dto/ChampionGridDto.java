package schultz.thomas.schub.core.lol.riot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// Moyennes des joueurs du champion dans un groupe de paliers. Métrique absente : pas assez de joueurs.
public record ChampionGridDto(int championId, @Nullable String group, List<String> patches, Instant computedAt,
                              List<Double> percentiles, Map<String, Metric> metrics) {

    @Schema(name = "ChampionGridMetric")
    public record Metric(String polarity, long count, List<Double> values) {
    }
}
