package schultz.thomas.schub.core.lol.stats.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.stats.business.model.TeamSynergy;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SoutiensTest {

    private static final List<String> POSTES = Soutiens.POSTES;

    // Nos joueurs sont en bleu : b-<poste> ; les rouges : r-<poste>.
    private static RiotStatsGateway.Insight insight(String id, List<RiotStatsGateway.Isolation> isolements,
                                                    List<RiotStatsGateway.Proximity> paires,
                                                    List<RiotStatsGateway.Contest> contests) {
        List<RiotStatsGateway.InsightPlayer> joueurs = IntStream.range(0, 10)
                .mapToObj(i -> new RiotStatsGateway.InsightPlayer((i < 5 ? "b-" : "r-") + POSTES.get(i % 5),
                        i < 5 ? 100 : 200, POSTES.get(i % 5), 10 + i, null, null, null, null))
                .toList();
        return new RiotStatsGateway.Insight(id, true, null, null, joueurs, null, paires, isolements, contests);
    }

    private static RiotStatsGateway.SharedMatch partie(String id) {
        return new RiotStatsGateway.SharedMatch(id, null, 1800, 420, "RANKED_FLEX", "16.19", 5, false, true,
                List.of(new RiotStatsGateway.SharedMatchPlayer("b-UTILITY", 1, "A", "UTILITY", true, 100, 0, 0, 0, 0,
                        0, 0, 0, 0, false, null)), List.of());
    }

    private static RiotStatsGateway.Isolation seul(String poste, int avant, int seulAvant, int apres, int seulApres) {
        return new RiotStatsGateway.Isolation("b-" + poste, 100, avant, seulAvant, apres, seulApres);
    }

    private static RiotStatsGateway.Contest objectif(int camp, String... postes) {
        return new RiotStatsGateway.Contest(600, "DRAGON", camp, 0, 0, presents(postes));
    }

    private static RiotStatsGateway.Contest combat(int camp, int kills, int morts, String... postes) {
        return new RiotStatsGateway.Contest(600, RiotStatsGateway.Contest.JUNGLE_FIGHT, camp, kills, morts,
                presents(postes));
    }

    private static List<String> presents(String... postes) {
        List<String> puuids = new ArrayList<>();
        for (String p : postes) {
            puuids.add(p.startsWith("r-") ? p : "b-" + p);
        }
        return puuids;
    }

    private static TeamMember membre(String id, String puuid) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(id);
        membre.setRiotPuuid(puuid);
        return membre;
    }

    @Test
    @DisplayName("Par poste : temps seul, objectifs pris à trois ou plus et avec le support, combats du jungler dans sa jungle")
    void parPoste() {
        RiotStatsGateway.Insight g1 = insight("g1",
                List.of(seul("BOTTOM", 14, 2, 10, 6), seul("UTILITY", 14, 1, 10, 2), seul("JUNGLE", 14, 7, 10, 3)),
                List.of(new RiotStatsGateway.Proximity("b-JUNGLE", "b-UTILITY", 100, 14, 2, 10, 6, 3000, 0)),
                List.of(objectif(100, "JUNGLE", "UTILITY", "MIDDLE", "r-JUNGLE"),
                        objectif(200, "JUNGLE"),
                        objectif(100, "JUNGLE", "UTILITY"),
                        combat(100, 0, 1, "JUNGLE", "r-JUNGLE", "r-MIDDLE"),
                        combat(100, 2, 0, "JUNGLE", "UTILITY", "r-JUNGLE"),
                        combat(200, 1, 1, "JUNGLE", "r-JUNGLE"),
                        combat(100, 1, 0, "UTILITY", "r-TOP")));

        TeamSynergy.Roles roles = Soutiens.equipe(List.of(partie("g1")), Map.of("g1", g1),
                Map.of("b-UTILITY", membre("sup", "b-UTILITY")), Map.of("sup", "Sup"));

        assertThat(roles.games()).isEqualTo(1);
        assertThat(roles.objectives()).isEqualTo(3);
        assertThat(roles.jungleFights()).isEqualTo(2);
        TeamSynergy.RoleLine adc = roles.roles().stream().filter(r -> r.position().equals("BOTTOM")).findFirst().orElseThrow();
        assertThat(adc.lateAlone()).isCloseTo(0.6, within(1e-9));
        assertThat(adc.objectivePresence()).isZero();
        TeamSynergy.RoleLine sup = roles.roles().stream().filter(r -> r.position().equals("UTILITY")).findFirst().orElseThrow();
        assertThat(sup.objectivePresence()).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(sup.jungleFightPresence()).isCloseTo(0.5, within(1e-9));
        assertThat(roles.takes()).isEqualTo(new TeamSynergy.ObjectiveTakes(1, 1, 2, 1, 2, 2, 1, 0));
        assertThat(roles.fights()).isEqualTo(new TeamSynergy.JungleFights(1, 1, 1, 0));
        assertThat(roles.pairs()).singleElement().satisfies(p -> {
            assertThat(p.position()).isEqualTo("JUNGLE");
            assertThat(p.matePosition()).isEqualTo("UTILITY");
            assertThat(p.lateShare()).isCloseTo(0.6, within(1e-9));
        });
        assertThat(roles.holders()).singleElement().isEqualTo(new TeamSynergy.Holder("UTILITY", "sup", "Sup", 1,
                0.2, 0.6, 2.0 / 3));
    }

    @Test
    @DisplayName("Une partie dont un de nos postes manque ne compte pas")
    void postesIncomplets() {
        RiotStatsGateway.Insight g1 = insight("g1", List.of(), List.of(), List.of(objectif(100, "JUNGLE")));
        List<RiotStatsGateway.InsightPlayer> sansSupport = g1.participants().stream()
                .filter(j -> !j.puuid().equals("b-UTILITY")).toList();
        RiotStatsGateway.Insight incomplet = new RiotStatsGateway.Insight("g1", true, null, null, sansSupport, null,
                List.of(), List.of(), g1.contests());

        TeamSynergy.Roles roles = Soutiens.equipe(List.of(partie("g1")), Map.of("g1", incomplet), Map.of(), Map.of());

        assertThat(roles.games()).isZero();
        assertThat(roles.objectives()).isZero();
        assertThat(roles.roles()).allSatisfy(r -> assertThat(r.objectivePresence()).isNull());
    }
}
