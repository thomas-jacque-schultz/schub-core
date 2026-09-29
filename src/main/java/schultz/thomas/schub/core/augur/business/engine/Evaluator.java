package schultz.thomas.schub.core.augur.business.engine;

import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Logique floue légère : chaque condition rend un degré entre 0 et 1 par une rampe. Obligatoires : le minimum
 * (un « et » flou). Optionnelles : moyenne pondérée, qui retire au plus {@code optionalInfluence} du degré.
 * Une exception satisfaite (degré ≥ 0,5) annule la règle. Le pattern est émis au-delà de son seuil, et son
 * degré devient sa confiance. Pas d'inférence complète (Mamdani) : chaque constat doit se justifier.
 */
public final class Evaluator {

    public static final String PATTERN_SIGNAL = "pattern:";
    private static final double EXCEPTION_SATISFAITE = 0.5;

    public enum Role { REQUIRED, OPTIONAL, EXCEPTION }

    // observed : ce que la condition compare (le centile pour une condition en centile) ; value : la mesure brute.
    public record ConditionTrace(Role role, String signal, Condition.Unit unit, double from, double to,
                                 Double observed, Double value, Double degree) {
    }

    public record Evaluation(String patternKey, int version, double degree, boolean emitted, boolean excepted,
                             List<ConditionTrace> conditions) {
    }

    private Evaluator() {
    }

    public static Evaluation evaluate(PatternVersion pattern, Signals signals) {
        List<ConditionTrace> traces = new ArrayList<>();

        double requis = 1;
        boolean evaluable = !pattern.getRequired().isEmpty();
        for (Condition condition : pattern.getRequired()) {
            Optional<Double> degre = degree(condition, signals);
            traces.add(trace(Role.REQUIRED, condition, signals, degre));
            if (degre.isEmpty()) {
                evaluable = false;
            } else {
                requis = Math.min(requis, degre.get());
            }
        }

        double somme = 0;
        double poids = 0;
        for (Condition condition : pattern.getOptional()) {
            Optional<Double> degre = degree(condition, signals);
            traces.add(trace(Role.OPTIONAL, condition, signals, degre));
            if (degre.isPresent()) {
                somme += degre.get() * condition.weightOrOne();
                poids += condition.weightOrOne();
            }
        }

        boolean excepte = false;
        for (Condition condition : pattern.getExceptions()) {
            Optional<Double> degre = degree(condition, signals);
            traces.add(trace(Role.EXCEPTION, condition, signals, degre));
            if (degre.isPresent() && degre.get() >= EXCEPTION_SATISFAITE) {
                excepte = true;
            }
        }

        double degre = !evaluable || excepte ? 0 : requis;
        if (degre > 0 && poids > 0) {
            double influence = clamp(pattern.getOptionalInfluence());
            degre = degre * (1 - influence + influence * (somme / poids));
        }
        return new Evaluation(pattern.getKey(), pattern.getVersion(), degre,
                degre >= pattern.getThreshold() && degre > 0, excepte, traces);
    }

    // Les patterns qui lisent d'autres patterns (signal « pattern:clé ») passent après eux.
    public static Map<String, Evaluation> evaluateAll(List<PatternVersion> patterns, Signals signals) {
        Map<String, Evaluation> resultats = new LinkedHashMap<>();
        List<PatternVersion> restants = new ArrayList<>(patterns);
        boolean progres = true;
        while (!restants.isEmpty() && progres) {
            progres = false;
            for (PatternVersion pattern : List.copyOf(restants)) {
                if (dependancesPretes(pattern, resultats, restants)) {
                    Evaluation evaluation = evaluate(pattern, signals);
                    resultats.put(pattern.getKey(), evaluation);
                    signals.put(PATTERN_SIGNAL + pattern.getKey(), evaluation.degree(), null);
                    restants.remove(pattern);
                    progres = true;
                }
            }
        }
        // Un cycle : ces patterns s'évaluent sans leurs dépendances, qui manqueront à leur preuve.
        restants.forEach(pattern -> resultats.put(pattern.getKey(), evaluate(pattern, signals)));
        return resultats;
    }

    private static boolean dependancesPretes(PatternVersion pattern, Map<String, Evaluation> faits,
                                             List<PatternVersion> restants) {
        return conditions(pattern).stream()
                .map(Condition::signal)
                .filter(signal -> signal.startsWith(PATTERN_SIGNAL))
                .map(signal -> signal.substring(PATTERN_SIGNAL.length()))
                .allMatch(cle -> faits.containsKey(cle)
                        || restants.stream().noneMatch(autre -> autre.getKey().equals(cle)));
    }

    private static List<Condition> conditions(PatternVersion pattern) {
        List<Condition> toutes = new ArrayList<>(pattern.getRequired());
        toutes.addAll(pattern.getOptional());
        toutes.addAll(pattern.getExceptions());
        return toutes;
    }

    static Optional<Double> degree(Condition condition, Signals signals) {
        return observed(condition, signals).map(x -> ramp(x, condition.from(), condition.to()));
    }

    private static Optional<Double> observed(Condition condition, Signals signals) {
        return signals.get(condition.signal()).flatMap(signal -> condition.unit() == Condition.Unit.PERCENTILE
                ? Optional.ofNullable(signal.percentile())
                : Optional.of(signal.value()));
    }

    static double ramp(double x, double from, double to) {
        if (from == to) {
            return x >= to ? 1 : 0;
        }
        return clamp((x - from) / (to - from));
    }

    private static ConditionTrace trace(Role role, Condition condition, Signals signals, Optional<Double> degre) {
        return new ConditionTrace(role, condition.signal(), condition.unit(), condition.from(), condition.to(),
                observed(condition, signals).orElse(null),
                signals.get(condition.signal()).map(Signals.Signal::value).orElse(null), degre.orElse(null));
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
