package schultz.thomas.schub.core.lol.stats.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.stats.business.model.EarlyGameView;
import schultz.thomas.schub.core.lol.stats.business.model.TeamSynergy;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProximitesTest {

    private static final List<String> POSTES = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

    private static RiotStatsGateway.Insight insight(String id, List<RiotStatsGateway.Proximity> paires) {
        List<RiotStatsGateway.InsightPlayer> joueurs = IntStream.range(0, 10)
                .mapToObj(i -> new RiotStatsGateway.InsightPlayer((i < 5 ? "b-" : "r-") + POSTES.get(i % 5),
                        i < 5 ? 100 : 200, POSTES.get(i % 5), 10 + i, null, null, null, null))
                .toList();
        return new RiotStatsGateway.Insight(id, true, null, null, joueurs, null, paires);
    }

    private static RiotStatsGateway.Proximity paire(String a, String b, int avant, int ensembleAvant, int apres,
                                                    int ensembleApres, int departs) {
        return new RiotStatsGateway.Proximity(a, b, a.startsWith("b-") ? 100 : 200, avant, ensembleAvant, apres,
                ensembleApres, 3000, departs);
    }

    private static TeamMember membre(String id, String puuid) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(id);
        membre.setRiotPuuid(puuid);
        return membre;
    }

    @Test
    @DisplayName("Le support : avec l'ADC avant 15 min, ses départs, et avec le jungler après")
    void support() {
        RiotStatsGateway.Insight partie = insight("g", List.of(
                paire("b-BOTTOM", "b-UTILITY", 12, 9, 10, 2, 3),
                paire("b-JUNGLE", "b-UTILITY", 12, 1, 10, 4, 0)));

        EarlyGameView.SupportLine ligne = Proximites.support(partie, 100,
                Map.of("b-UTILITY", membre("m-sup", "b-UTILITY")));

        assertThat(ligne.memberId()).isEqualTo("m-sup");
        assertThat(ligne.championId()).isEqualTo(14);
        assertThat(ligne.adcEarlyShare()).isCloseTo(0.75, within(1e-9));
        assertThat(ligne.roams()).isEqualTo(3);
        assertThat(ligne.junglerLateShare()).isCloseTo(0.4, within(1e-9));
        assertThat(Proximites.support(partie, 200, Map.of())).isNull();
    }

    @Test
    @DisplayName("Le jungler : le poste dont il a été le plus proche sur toute la partie")
    void jungler() {
        RiotStatsGateway.Insight partie = insight("g", List.of(
                paire("b-TOP", "b-JUNGLE", 14, 2, 10, 1, 0),
                paire("b-JUNGLE", "b-MIDDLE", 14, 6, 10, 6, 0),
                paire("b-JUNGLE", "b-UTILITY", 14, 3, 10, 7, 0)));

        EarlyGameView.JunglerLine ligne = Proximites.jungler(partie, 100, Map.of());

        assertThat(ligne.position()).isEqualTo("MIDDLE");
        assertThat(ligne.share()).isCloseTo(0.5, within(1e-9));
        assertThat(ligne.memberId()).isNull();
    }

    @Test
    @DisplayName("Dans l'équipe, les parts se groupent sur les parties communes ; sous cinq parties, pas de part")
    void equipe() {
        Map<String, TeamMember> parPuuid = new LinkedHashMap<>();
        parPuuid.put("b-BOTTOM", membre("adc", "b-BOTTOM"));
        parPuuid.put("b-UTILITY", membre("sup", "b-UTILITY"));
        parPuuid.put("b-TOP", membre("top", "b-TOP"));
        List<RiotStatsGateway.SharedMatch> parties = IntStream.range(0, 6)
                .mapToObj(i -> new RiotStatsGateway.SharedMatch("g" + i, null, 1800, 420, "RANKED_FLEX", "16.19", 4,
                        false, true, List.of(), List.of()))
                .toList();
        Map<String, RiotStatsGateway.Insight> insights = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            List<RiotStatsGateway.Proximity> paires = i < 2
                    ? List.of(paire("b-BOTTOM", "b-UTILITY", 10, 8, 10, 4, 1), paire("b-TOP", "b-UTILITY", 10, 0, 10, 2, 0))
                    : List.of(paire("b-BOTTOM", "b-UTILITY", 10, 6, 10, 4, 1));
            insights.put("g" + i, insight("g" + i, paires));
        }

        List<TeamSynergy.Closeness> proches = Proximites.equipe(parties, insights, parPuuid,
                Map.of("adc", "Adc", "sup", "Sup", "top", "Top"));

        TeamSynergy.Closeness sup = proches.stream().filter(c -> c.memberId().equals("sup")).findFirst().orElseThrow();
        assertThat(sup.mates()).extracting(TeamSynergy.Mate::memberId).containsExactly("adc", "top");
        TeamSynergy.Mate adc = sup.mates().getFirst();
        assertThat(adc.games()).isEqualTo(6);
        assertThat(adc.earlyShare()).isCloseTo(40.0 / 60, within(1e-9));
        assertThat(adc.lateShare()).isCloseTo(0.4, within(1e-9));
        TeamSynergy.Mate top = sup.mates().get(1);
        assertThat(top.games()).isEqualTo(2);
        assertThat(top.share()).isNull();
    }
}
