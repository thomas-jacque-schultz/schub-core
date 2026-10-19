package schultz.thomas.schub.core.lol.session.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionWatcherTest {

    private static RiotStatsGateway.SharedMatch partie(String id, String debut) {
        return new RiotStatsGateway.SharedMatch(id, Instant.parse(debut), 1800, 420, "RANKED_FLEX", "16.19", 3, false,
                true, List.of(), List.of());
    }

    @Test
    @DisplayName("deux parties à moins d'une heure d'écart font une session ; une partie isolée n'en fait pas")
    void groupes() {
        List<List<RiotStatsGateway.SharedMatch>> groupes = SessionWatcher.groupes(List.of(
                partie("a", "2026-10-05T19:00:00Z"),
                partie("b", "2026-10-05T19:45:00Z"),
                partie("c", "2026-10-05T22:00:00Z")));

        assertThat(groupes).hasSize(1);
        assertThat(groupes.get(0)).extracting(RiotStatsGateway.SharedMatch::matchId).containsExactly("a", "b");
    }
}
