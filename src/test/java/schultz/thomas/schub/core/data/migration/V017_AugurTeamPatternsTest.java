package schultz.thomas.schub.core.data.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class V017_AugurTeamPatternsTest {

    @Test
    @DisplayName("les constats d'équipe sont de portée TEAM, et leurs phrases ne citent que des signaux qu'ils lisent")
    void coherence() {
        Pattern reference = Pattern.compile("\\{([A-Za-z0-9]+)\\.(value|percentile|points)}");
        V017_AugurTeamPatterns.patterns().forEach(p -> {
            assertThat(p.getScope()).isEqualTo(PatternVersion.Scope.TEAM);
            var lus = p.getRequired().stream().map(Condition::signal).toList();
            p.getSentence().values().forEach(phrase -> {
                Matcher m = reference.matcher(phrase);
                while (m.find()) {
                    assertThat(lus).as("%s cite %s", p.getKey(), m.group(1)).contains(m.group(1));
                }
            });
        });
    }
}
