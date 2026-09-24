package schultz.thomas.schub.core.team.business.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Même interpolation que le connecteur et le front : sur un palier plat, la part des valeurs inférieures ou égales.
final class Notes {

    private Notes() {
    }

    static double repartition(List<Double> percentiles, List<Double> valeurs, double x) {
        if (x < valeurs.getFirst()) {
            return 0;
        }
        if (x >= valeurs.getLast()) {
            return 1;
        }
        int i = 0;
        while (valeurs.get(i + 1) <= x) {
            i++;
        }
        double v0 = valeurs.get(i);
        double v1 = valeurs.get(i + 1);
        return percentiles.get(i) + (percentiles.get(i + 1) - percentiles.get(i)) * (x - v0) / (v1 - v0);
    }

    static Double sens(Double p, String polarity) {
        return p == null ? null : "LOWER".equals(polarity) ? 1 - p : p;
    }

    // Le palier dont la moyenne est la plus proche ; sans moyennes, la métrique ne suit pas le rang.
    static String niveau(Double valeur, Map<String, Double> moyennes) {
        if (valeur == null || moyennes == null) {
            return null;
        }
        return moyennes.entrySet().stream()
                .min(Comparator.comparingDouble(palier -> Math.abs(palier.getValue() - valeur)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    static String groupe(String tier) {
        if (tier == null) {
            return null;
        }
        return List.of("MASTER", "GRANDMASTER", "CHALLENGER").contains(tier) ? "MASTER_PLUS" : tier;
    }
}
