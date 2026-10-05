package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DraftAdviceRulesTest {

    @Test
    @DisplayName("un 3/3 ne passe pas devant un 15/20 (borne basse de Wilson)")
    void wilson() {
        assertThat(DraftAdviceService.borneBasse(3, 3)).isLessThan(DraftAdviceService.borneBasse(15, 20));
        assertThat(DraftAdviceService.borneHaute(0, 2)).isGreaterThan(DraftAdviceService.borneHaute(2, 20));
    }

    private static RiotStatsGateway.SharedMatchPlayer joueur(int champion, int camp) {
        return new RiotStatsGateway.SharedMatchPlayer("p" + champion, champion, null, "TOP", true, camp, 0, 0, 0, 0,
                0, 0, 0, 0, false, null);
    }

    private static RiotStatsGateway.SharedMatch partie(List<Integer> membres, List<Integer> autres, int autreCamp) {
        return new RiotStatsGateway.SharedMatch("m", Instant.EPOCH, 1800, 420, "RANKED_SOLO", "16.19", 2, false, true,
                membres.stream().map(c -> joueur(c, 100)).toList(),
                autres.stream().map(c -> joueur(c, autreCamp)).toList());
    }

    @Test
    @DisplayName("une composition correspond quand ses cinq champions, ou leurs remplaçants, sont dans notre camp")
    void correspondance() {
        List<Set<Integer>> compo = List.of(Set.of(1), Set.of(2), Set.of(3, 30), Set.of(4), Set.of(5));

        assertThat(CompositionRecordService.correspond(partie(List.of(1, 2), List.of(30, 4, 5), 100), compo)).isTrue();
        assertThat(CompositionRecordService.correspond(partie(List.of(1, 2), List.of(3, 4, 5), 200), compo)).isFalse();
        assertThat(CompositionRecordService.correspond(partie(List.of(1, 2), List.of(3, 4, 6), 100), compo)).isFalse();
    }
}
