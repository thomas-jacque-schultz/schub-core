package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.Map;

public record MetricScaleDto(
        Instant computedAt,
        int population,
        int minimumGames,
        Map<String, Bound> bounds
) {

    public record Bound(double low, double high) {
    }
}
