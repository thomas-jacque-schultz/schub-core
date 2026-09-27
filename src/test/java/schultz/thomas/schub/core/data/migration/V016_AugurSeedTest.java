package schultz.thomas.schub.core.data.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.business.engine.Signals;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class V016_AugurSeedTest {

    private final List<PatternVersion> patterns = V016_AugurSeed.patterns();

    @Test
    @DisplayName("chaque pattern a une clé unique, une phrase et un libellé dans les deux langues")
    void completude() {
        assertThat(patterns.stream().map(PatternVersion::getKey).distinct()).hasSize(patterns.size());
        patterns.forEach(p -> {
            assertThat(p.getLabel()).containsKeys("fr", "en");
            assertThat(p.getSentence()).containsKeys("fr", "en");
        });
    }

    @Test
    @DisplayName("une phrase ne cite que des signaux que le pattern lit : sa preuve les contient")
    void phrasesCitentLaPreuve() {
        Pattern reference = Pattern.compile("\\{([A-Za-z0-9]+)\\.(value|percentile)}");
        patterns.forEach(p -> {
            List<String> lus = Stream.of(p.getRequired(), p.getOptional(), p.getExceptions())
                    .flatMap(List::stream).map(Condition::signal).toList();
            p.getSentence().values().forEach(phrase -> {
                Matcher m = reference.matcher(phrase);
                while (m.find()) {
                    assertThat(lus).as("%s cite %s", p.getKey(), m.group(1)).contains(m.group(1));
                }
            });
        });
    }

    @Test
    @DisplayName("un joueur qui meurt beaucoup sans vision reçoit le constat composé « joue sans information »")
    void composeSurDeVraisSignaux() {
        List<PatternVersion> habitudes = patterns.stream()
                .filter(p -> p.getScope() == PatternVersion.Scope.HABIT).collect(Collectors.toList());
        Signals signaux = new Signals().put("deathsPer10", 4.2, 92.0).put("visionPerMinute", 0.4, 8.0)
                .put("controlWardsPlaced", 0.2, 12.0).put("isSupport", 0, null);

        var r = Evaluator.evaluateAll(habitudes, signaux);

        assertThat(r.get("habitude-joue-sans-information").emitted()).isTrue();
        assertThat(r.get("habitude-style-prudent").emitted()).isFalse();
    }
}
