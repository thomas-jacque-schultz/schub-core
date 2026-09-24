package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.team.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.team.api.dto.TeamLevelDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TeamLevelsTest {

    private static final List<Double> P = List.of(0.0, 0.5, 1.0);
    private static final RiotStatsGateway.Standing OR = new RiotStatsGateway.Standing("RANKED_SOLO",
            "RANKED_SOLO_5x5", "GOLD", "II", 50, 10, 10, false, false, Instant.EPOCH);

    @Test
    @DisplayName("chaque partie se situe dans les parties de son palier ; la moyenne prend le palier le plus proche")
    void moyenneDesPercentiles() {
        Map<String, Double> moyennes = new LinkedHashMap<>();
        moyennes.put("IRON", -900.0);
        moyennes.put("GOLD", 100.0);
        moyennes.put("DIAMOND", 900.0);
        ReferenceGridDto grille = new ReferenceGridDto(List.of("16.18"), "TEAM", "TEAM", Instant.EPOCH, P,
                Map.of("goldDiffAt15", new ReferenceGridDto.Metric("HIGHER",
                                Map.of("GOLD", new ReferenceGridDto.Tier(500, List.of(-2000.0, 0.0, 2000.0))),
                                moyennes, List.of()),
                        "xpDiffAt15", new ReferenceGridDto.Metric("HIGHER",
                                Map.of("GOLD", new ReferenceGridDto.Tier(500, List.of(-2000.0, 0.0, 2000.0))),
                                null, List.of())));

        TeamLevelDto niveau = TeamLevels.of(List.of(partie("a"), partie("b")),
                Map.of("a", insight("a", 5200), "b", insight("b", 4800)), grille);

        TeamLevelDto.Metric or = niveau.metrics().stream().filter(m -> m.key().equals("goldDiffAt15")).findFirst()
                .orElseThrow();
        assertThat(or.games()).isEqualTo(2);
        assertThat(or.mean()).isCloseTo(0, within(1e-9));
        assertThat(or.inTier()).isCloseTo(0.5, within(1e-9));
        assertThat(or.level()).isEqualTo("GOLD");
        assertThat(niveau.tier()).isEqualTo("GOLD");
        assertThat(niveau.metrics()).filteredOn(m -> m.key().equals("xpDiffAt15")).singleElement()
                .satisfies(xp -> assertThat(xp.level()).isNull());
    }

    @Test
    @DisplayName("sans référentiel, les moyennes restent et les notes disparaissent")
    void sansGrille() {
        TeamLevelDto niveau = TeamLevels.of(List.of(partie("a")), Map.of("a", insight("a", 5200)), null);

        TeamLevelDto.Metric or = niveau.metrics().getFirst();
        assertThat(or.mean()).isCloseTo(1000, within(1e-9));
        assertThat(or.inTier()).isNull();
        assertThat(or.level()).isNull();
    }

    private static RiotStatsGateway.SharedMatch partie(String id) {
        RiotStatsGateway.SharedMatchPlayer nous = new RiotStatsGateway.SharedMatchPlayer("p0", 1, "X", "TOP", true,
                100, 0, 0, 0, 0, 0, 0, 0, 0, false);
        return new RiotStatsGateway.SharedMatch(id, Instant.EPOCH, 1800, 420, "RANKED_SOLO", "16.18", 5, false, true,
                List.of(nous), List.of());
    }

    // Notre camp à « or » par joueur, l'autre à 5000 : écart = 5 × (or − 5000).
    private static RiotStatsGateway.Insight insight(String id, int or) {
        List<RiotStatsGateway.InsightPlayer> joueurs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int cote = i < 5 ? 100 : 200;
            joueurs.add(new RiotStatsGateway.InsightPlayer("p" + i, cote, "TOP", 1, OR, null,
                    new RiotStatsGateway.At15(cote == 100 ? or : 5000, 6000, 100, 0, 0, 0, 0)));
        }
        RiotStatsGateway.EarlyGame early = new RiotStatsGateway.EarlyGame(List.of(), List.of(),
                List.of(new RiotStatsGateway.Objectives(100, 1, 3, 0)));
        return new RiotStatsGateway.Insight(id, true, Instant.EPOCH, early, joueurs);
    }
}
