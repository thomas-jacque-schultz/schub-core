package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.team.data.model.PremadeLabSettings;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisitorBudgetTest {

    private static final Instant T0 = Instant.parse("2026-09-27T12:00:00Z");

    private VisitorBudget budget(int parFenetre, int minutes) {
        PremadeLabSettings reglage = new PremadeLabSettings();
        reglage.setUnknownPlayerBudget(parFenetre);
        reglage.setBudgetWindowMinutes(minutes);
        PremadeLabSettingsService settings = mock(PremadeLabSettingsService.class);
        when(settings.current()).thenReturn(reglage);
        return new VisitorBudget(settings);
    }

    @Test
    @DisplayName("au-delà du budget, la demande passe en voie lente ; la fenêtre glisse")
    void fenetreGlissante() {
        VisitorBudget budget = budget(2, 10);

        assertThat(budget.consume("ip:1", T0)).isTrue();
        assertThat(budget.consume("ip:1", T0.plusSeconds(60))).isTrue();
        assertThat(budget.consume("ip:1", T0.plusSeconds(120))).isFalse();
        assertThat(budget.consume("ip:2", T0.plusSeconds(120))).isTrue();
        assertThat(budget.consume("ip:1", T0.plus(Duration.ofMinutes(10)).plusSeconds(1))).isTrue();
    }
}
