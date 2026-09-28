package schultz.thomas.schub.core.data.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.business.engine.Signals;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import static org.assertj.core.api.Assertions.assertThat;

class V020_AugurTimelineHabitsTest {

    private final PatternVersion mortsIsolees = V020_AugurTimelineHabits.patterns().getFirst();

    @Test
    @DisplayName("Une habitude de timeline ne sort qu'à partir de cinq parties analysées")
    void minimumDeParties() {
        assertThat(Evaluator.evaluate(mortsIsolees,
                new Signals().put("timelineGames", 4, null).put("isolatedDeathsAvg", 1.2, null)).emitted()).isFalse();
        assertThat(Evaluator.evaluate(mortsIsolees,
                new Signals().put("timelineGames", 8, null).put("isolatedDeathsAvg", 1.2, null)).emitted()).isTrue();
    }

    @Test
    @DisplayName("Sans timeline, rien n'est dit")
    void sansTimeline() {
        assertThat(Evaluator.evaluate(mortsIsolees, new Signals().put("deathsPer10", 9, 90.0)).emitted()).isFalse();
    }

    @Test
    @DisplayName("Chaque phrase cite le nombre de parties qui l'appuient, et ne cite que des signaux de ses conditions")
    void phrasesCitentLesParties() {
        V020_AugurTimelineHabits.patterns().forEach(p -> {
            assertThat(p.getSentence().get("fr")).contains("{timelineGames.value}");
            p.getSentence().values().forEach(phrase -> java.util.regex.Pattern.compile("\\{([A-Za-z0-9]+)\\.")
                    .matcher(phrase).results()
                    .forEach(m -> assertThat(p.getRequired()).anyMatch(c -> c.signal().equals(m.group(1)))));
        });
    }
}
