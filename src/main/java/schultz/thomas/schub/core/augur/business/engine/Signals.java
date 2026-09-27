package schultz.thomas.schub.core.augur.business.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Ce que les capteurs ont mesuré sur un sujet : valeur brute, et son centile dans le référentiel du palier.
public final class Signals {

    public record Signal(double value, Double percentile) {
    }

    private final Map<String, Signal> values = new LinkedHashMap<>();
    private final Map<String, String> context = new LinkedHashMap<>();

    public Signals put(String key, double value, Double percentile) {
        values.put(key, new Signal(value, percentile));
        return this;
    }

    public Signals context(String key, String value) {
        if (value != null) {
            context.put(key, value);
        }
        return this;
    }

    public Optional<Signal> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public Map<String, Signal> all() {
        return values;
    }

    public Map<String, String> context() {
        return context;
    }
}
