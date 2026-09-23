package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsWindowsTest {

    @Mock private RiotStatsGateway statsGateway;
    @InjectMocks private StatsWindows windows;

    @Test
    @DisplayName("les 2 derniers patchs = les jours depuis la première partie du plus ancien des deux")
    void patchsEnJours() {
        Instant maintenant = Instant.now();
        when(statsGateway.patches(2)).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.PatchStart("16.19", maintenant.minus(Duration.ofDays(5))),
                new RiotStatsGateway.PatchStart("16.18", maintenant.minus(Duration.ofDays(19))))));

        assertThat(windows.days(null, 2)).isEqualTo(20);
    }

    @Test
    @DisplayName("sans patchs demandés, les jours passent tels quels ; connecteur muet, aussi")
    void joursTelsQuels() {
        assertThat(windows.days(90, null)).isEqualTo(90);
        verifyNoInteractions(statsGateway);

        when(statsGateway.patches(4)).thenReturn(Optional.empty());
        assertThat(windows.days(null, 4)).isNull();
    }
}
