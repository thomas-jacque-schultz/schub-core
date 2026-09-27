package schultz.thomas.schub.core.data.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class V018_AugurTimelineAndBuildTest {

    @Test
    @DisplayName("les constats de timeline et de build sont de portée GAME, et leurs phrases ne citent que des signaux qu'ils lisent")
    void coherence() {
        Pattern reference = Pattern.compile("\\{([A-Za-z0-9]+)\\.(value|percentile|points)}");
        V018_AugurTimelineAndBuild.patterns().forEach(p -> {
            assertThat(p.getScope()).isEqualTo(PatternVersion.Scope.GAME);
            var lus = p.getRequired().stream().map(Condition::signal).toList();
            p.getSentence().values().forEach(phrase -> {
                Matcher m = reference.matcher(phrase);
                while (m.find()) {
                    assertThat(lus).as("%s cite %s", p.getKey(), m.group(1)).contains(m.group(1));
                }
            });
        });
    }

    @Test
    @DisplayName("armure sans anti-critique : émis face à deux porteurs, annulé par un Randuin")
    void armureContreCritique() {
        PatternVersion regle = V018_AugurTimelineAndBuild.patterns().stream()
                .filter(p -> p.getKey().equals("build-armure-contre-critique")).findFirst().orElseThrow();
        var sans = new schultz.thomas.schub.core.augur.business.engine.Signals()
                .put("enemyCritCarries", 2, null).put("frontlineArmored", 1, null).put("hasAntiCrit", 0, null);
        var avec = new schultz.thomas.schub.core.augur.business.engine.Signals()
                .put("enemyCritCarries", 2, null).put("frontlineArmored", 1, null).put("hasAntiCrit", 1, null);

        assertThat(schultz.thomas.schub.core.augur.business.engine.Evaluator.evaluate(regle, sans).emitted()).isTrue();
        assertThat(schultz.thomas.schub.core.augur.business.engine.Evaluator.evaluate(regle, avec).emitted()).isFalse();
    }
}
