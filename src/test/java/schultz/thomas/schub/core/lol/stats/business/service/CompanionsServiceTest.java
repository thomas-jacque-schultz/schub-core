package schultz.thomas.schub.core.lol.stats.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.stats.business.model.Companions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanionsServiceTest {

    private static RiotStatsGateway.SharedMatchPlayer joueur(String puuid, int camp) {
        return new RiotStatsGateway.SharedMatchPlayer(puuid, 1, null, "TOP", true, camp, 0, 0, 0, 0, 0, 0, 0, 0,
                false, puuid + "#EUW");
    }

    private static RiotStatsGateway.SharedMatch partie(boolean victoire, String... coequipiers) {
        List<RiotStatsGateway.SharedMatchPlayer> autres = new ArrayList<>();
        for (String c : coequipiers) {
            autres.add(joueur(c, 100));
        }
        autres.add(joueur("adverse", 200));
        return new RiotStatsGateway.SharedMatch("m", Instant.EPOCH, 1800, 420, "RANKED_SOLO", "16.19", 1, false,
                victoire, List.of(joueur("moi", 100)), autres);
    }

    @Test
    @DisplayName("au moins 5 parties ensemble ; trois compagnons réunis 5 fois donnent l'équipe à proposer")
    void compagnons() {
        List<RiotStatsGateway.SharedMatch> parties = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            parties.add(partie(i < 4, "a", "b", "c"));
        }
        parties.add(partie(true, "a", "d"));
        parties.add(partie(false, "d"));

        Companions dto = CompanionsService.calcule("moi", "2026-10", parties);

        assertThat(dto.companions()).extracting(Companions.Companion::riotId)
                .containsExactly("a#EUW", "b#EUW", "c#EUW");
        assertThat(dto.companions().get(0).games()).isEqualTo(6);
        assertThat(dto.companions().get(0).withoutWinRate()).isNull();
        assertThat(dto.suggestedTeam()).containsExactlyInAnyOrder("a#EUW", "b#EUW", "c#EUW");
    }
}
