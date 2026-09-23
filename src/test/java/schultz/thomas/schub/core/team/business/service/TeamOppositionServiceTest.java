package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import schultz.thomas.schub.core.team.api.dto.AverageRankDto;
import schultz.thomas.schub.core.team.api.dto.PositionOppositionDto;
import schultz.thomas.schub.core.team.api.dto.TeamOppositionDto;
import schultz.thomas.schub.core.team.api.dto.TeamRecordDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TeamOppositionServiceTest {

    private static final Instant QUAND = Instant.parse("2026-09-01T18:00:00Z");
    private static final Set<String> MEMBRES = Set.of("m1", "m2", "m3", "m4");
    private static final List<String> POSTES = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

    private static RiotStatsGateway.Standing solo(String tier, String division) {
        return new RiotStatsGateway.Standing("RANKED_SOLO", "RANKED_SOLO_5x5", tier, division, 0, 0, 0,
                false, false, QUAND);
    }

    private static RiotStatsGateway.SharedMatch partie(String id, boolean win) {
        return new RiotStatsGateway.SharedMatch(id, QUAND, 1800, 440, "RANKED_FLEX", "16.18", 4, false, win,
                List.of(new RiotStatsGateway.SharedMatchPlayer("m1", 1, "A", "TOP", win, 100, 0, 0, 0, 0, 0, 0,
                        0, 0, false)), List.of());
    }

    // Notre camp en Or II, le leur au palier donné ; le top membre face au top adverse.
    private static RiotStatsGateway.Insight insight(String id, String tierAdverse, int orTop) {
        List<RiotStatsGateway.InsightPlayer> joueurs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String puuid = i < 4 ? "m" + (i + 1) : "allie";
            RiotStatsGateway.At15 a15 = i == 0 ? new RiotStatsGateway.At15(5000 + orTop, 0, 120, 0, 0, 0, 0) : null;
            joueurs.add(new RiotStatsGateway.InsightPlayer(puuid, 100, POSTES.get(i), 1, solo("GOLD", "II"), null, a15));
            RiotStatsGateway.At15 a15Adverse = i == 0 ? new RiotStatsGateway.At15(5000, 0, 110, 0, 0, 0, 0) : null;
            joueurs.add(new RiotStatsGateway.InsightPlayer("e" + i, 200, POSTES.get(i), 2,
                    tierAdverse == null ? null : solo(tierAdverse, "II"), null, a15Adverse));
        }
        return new RiotStatsGateway.Insight(id, true, QUAND.plusSeconds(3 * 86_400), joueurs);
    }

    @Test
    @DisplayName("Moyenne des classés seulement : un non-classé ne tire pas la moyenne vers le Fer")
    void moyenneDesClasses() {
        AverageRankDto moyenne = Rangs.moyenne(Arrays.asList(solo("DIAMOND", "IV"), null, null));
        assertThat(moyenne.tier()).isEqualTo("DIAMOND");
        assertThat(moyenne.counted()).isEqualTo(1);
    }

    @Test
    @DisplayName("Paliers adverses, écarts, plafond et couloirs gagnés à 15 minutes")
    void calcule() {
        List<RiotStatsGateway.SharedMatch> parties = new ArrayList<>();
        Map<String, RiotStatsGateway.Insight> insights = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            parties.add(partie("g" + i, i < 4));
            insights.put("g" + i, insight("g" + i, "GOLD", 300));
        }
        for (int i = 6; i < 11; i++) {
            parties.add(partie("g" + i, i == 6));
            insights.put("g" + i, insight("g" + i, "PLATINUM", -200));
        }
        parties.add(partie("sans-rangs", true));

        TeamOppositionDto dto = TeamOppositionService.calcule("equipe-1", null, parties, insights, MEMBRES);

        assertThat(dto.games()).isEqualTo(12);
        assertThat(dto.gamesWithRanks()).isEqualTo(11);
        assertThat(dto.medianLagDays()).isCloseTo(3.0, within(1e-9));
        assertThat(dto.byEnemyTier()).extracting(TeamRecordDto::key).containsExactly("GOLD", "PLATINUM");
        assertThat(dto.ceilingTier()).isEqualTo("GOLD");
        assertThat(dto.byGap()).extracting(TeamRecordDto::key).containsExactly("MUCH_STRONGER", "EVEN");

        PositionOppositionDto top = dto.byPosition().getFirst();
        assertThat(top.position()).isEqualTo("TOP");
        assertThat(top.laneGames()).isEqualTo(11);
        assertThat(top.laneWinRate()).isCloseTo(6 / 11.0, within(1e-9));
        assertThat(top.versusStronger().games()).isEqualTo(5);
    }
}
