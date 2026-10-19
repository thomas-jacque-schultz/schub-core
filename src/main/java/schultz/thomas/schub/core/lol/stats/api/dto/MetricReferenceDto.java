package schultz.thomas.schub.core.lol.stats.api.dto;

import java.util.Map;

// 5e et 95e percentiles des moyennes par joueur d'une population, au même poste.
public record MetricReferenceDto(String tier, String position, int population, int minimumGames,
                                 Map<String, Bound> bounds) {

    public record Bound(double low, double high) {
    }
}
