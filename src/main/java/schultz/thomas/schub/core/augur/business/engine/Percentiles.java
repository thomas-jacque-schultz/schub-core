package schultz.thomas.schub.core.augur.business.engine;

import java.util.List;

public final class Percentiles {

    private Percentiles() {
    }

    // Le centile (0 à 100) d'une valeur, par interpolation entre les valeurs de la grille à chaque centile.
    public static Double of(double x, List<Double> percentiles, List<Double> values) {
        if (percentiles == null || values == null || values.isEmpty() || percentiles.size() != values.size()) {
            return null;
        }
        if (x <= values.get(0)) {
            return percentiles.get(0) * 100;
        }
        for (int i = 1; i < values.size(); i++) {
            double bas = values.get(i - 1);
            double haut = values.get(i);
            if (x <= haut) {
                double part = haut == bas ? 1 : (x - bas) / (haut - bas);
                return (percentiles.get(i - 1) + part * (percentiles.get(i) - percentiles.get(i - 1))) * 100;
            }
        }
        return percentiles.get(percentiles.size() - 1) * 100;
    }
}
