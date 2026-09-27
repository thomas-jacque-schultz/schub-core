package schultz.thomas.schub.core.augur.business.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EvaluatorTest {

    private static PatternVersion pattern(String key, List<Condition> requises, List<Condition> optionnelles,
                                          List<Condition> exceptions) {
        PatternVersion p = new PatternVersion();
        p.setKey(key);
        p.setVersion(1);
        p.setRequired(requises);
        p.setOptional(optionnelles);
        p.setExceptions(exceptions);
        return p;
    }

    private static Condition pct(String signal, double from, double to) {
        return new Condition(signal, Condition.Unit.PERCENTILE, from, to, 1);
    }

    @Test
    @DisplayName("une rampe : 0 sous le 60e centile, 1 au-dessus du 80e, entre les deux ça monte")
    void rampe() {
        PatternVersion morts = pattern("morts", List.of(pct("deathsPer10", 60, 80)), List.of(), List.of());

        assertThat(Evaluator.evaluate(morts, new Signals().put("deathsPer10", 7, 55.0)).degree()).isZero();
        assertThat(Evaluator.evaluate(morts, new Signals().put("deathsPer10", 8, 70.0)).degree()).isCloseTo(0.5, within(1e-9));
        assertThat(Evaluator.evaluate(morts, new Signals().put("deathsPer10", 9, 90.0)).emitted()).isTrue();
    }

    @Test
    @DisplayName("4,9 et 5,1 morts ne donnent pas deux conclusions opposées : le degré varie peu")
    void pasDeClignotement() {
        PatternVersion morts = pattern("morts", List.of(new Condition("deaths", Condition.Unit.VALUE, 3, 7, 1)),
                List.of(), List.of());

        double a = Evaluator.evaluate(morts, new Signals().put("deaths", 4.9, null)).degree();
        double b = Evaluator.evaluate(morts, new Signals().put("deaths", 5.1, null)).degree();
        assertThat(Math.abs(a - b)).isLessThan(0.06);
    }

    @Test
    @DisplayName("obligatoires : le minimum ; une rampe descendante monte quand le signal baisse")
    void etFlou() {
        PatternVersion p = pattern("sans-info", List.of(pct("vision", 40, 15), pct("deaths", 60, 80)), List.of(),
                List.of());

        Evaluator.Evaluation e = Evaluator.evaluate(p, new Signals().put("vision", 0, 20.0).put("deaths", 0, 90.0));
        assertThat(e.degree()).isCloseTo(0.8, within(1e-9));
    }

    @Test
    @DisplayName("les optionnelles modulent le degré sans l'annuler, une exception l'annule")
    void optionnellesEtExceptions() {
        PatternVersion p = pattern("farm", List.of(pct("cs", 60, 85)), List.of(pct("gold", 60, 85)),
                List.of(new Condition("isSupport", Condition.Unit.VALUE, 0, 1, 1)));

        assertThat(Evaluator.evaluate(p, new Signals().put("cs", 0, 90.0).put("gold", 0, 10.0).put("isSupport", 0, null))
                .degree()).isCloseTo(0.7, within(1e-9));
        Evaluator.Evaluation support = Evaluator.evaluate(p,
                new Signals().put("cs", 0, 90.0).put("gold", 0, 90.0).put("isSupport", 1, null));
        assertThat(support.degree()).isZero();
        assertThat(support.excepted()).isTrue();
    }

    @Test
    @DisplayName("un signal obligatoire absent : rien n'est conclu, et la trace le dit")
    void signalAbsent() {
        Evaluator.Evaluation e = Evaluator.evaluate(pattern("x", List.of(pct("absent", 0, 10)), List.of(), List.of()),
                new Signals());

        assertThat(e.emitted()).isFalse();
        assertThat(e.conditions().get(0).observed()).isNull();
    }

    @Test
    @DisplayName("chaînage : un pattern composé lit le degré des patterns dont il dépend, quel que soit l'ordre")
    void chainage() {
        PatternVersion compose = pattern("sans-information",
                List.of(new Condition("pattern:vision-faible", Condition.Unit.VALUE, 0.3, 0.7, 1),
                        new Condition("pattern:morts", Condition.Unit.VALUE, 0.3, 0.7, 1)), List.of(), List.of());
        PatternVersion vision = pattern("vision-faible", List.of(pct("vision", 40, 15)), List.of(), List.of());
        PatternVersion morts = pattern("morts", List.of(pct("deaths", 60, 80)), List.of(), List.of());

        Map<String, Evaluator.Evaluation> r = Evaluator.evaluateAll(List.of(compose, vision, morts),
                new Signals().put("vision", 0, 10.0).put("deaths", 0, 95.0));

        assertThat(r.get("sans-information").emitted()).isTrue();
    }

    @Test
    @DisplayName("un centile se lit par interpolation dans la grille du palier")
    void centile() {
        assertThat(Percentiles.of(5, List.of(0.0, 0.5, 1.0), List.of(0.0, 4.0, 8.0))).isCloseTo(62.5, within(1e-9));
        assertThat(Percentiles.of(-1, List.of(0.0, 0.5, 1.0), List.of(0.0, 4.0, 8.0))).isZero();
        assertThat(Percentiles.of(99, List.of(0.0, 0.5, 1.0), List.of(0.0, 4.0, 8.0))).isEqualTo(100.0);
    }
}
