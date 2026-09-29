package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.team.api.dto.TeamSynergyDto;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TeamSynergyServiceTest {

    private static RiotStatsGateway.SharedMatchPlayer joueur(String puuid, String poste, int or, int degats) {
        return new RiotStatsGateway.SharedMatchPlayer(puuid, 1, "X", poste, true, 100, 0, 0, 0, 0, or, degats, 0, 0,
                false, null);
    }

    private static RiotStatsGateway.SharedMatch partie(boolean victoire, List<String> presents) {
        List<RiotStatsGateway.SharedMatchPlayer> membres = presents.stream()
                .map(p -> joueur(p, p.equals("adc") ? "BOTTOM" : "MIDDLE", p.equals("adc") ? 4000 : 2000, 1000))
                .toList();
        return new RiotStatsGateway.SharedMatch("m", null, 1800, 420, "RANKED_FLEX", "16.19", presents.size(), false,
                victoire, membres, List.of(joueur("allie", "UTILITY", 2000, 1000)));
    }

    private static Map<String, TeamMember> effectif(String... puuids) {
        Map<String, TeamMember> m = new LinkedHashMap<>();
        for (String p : puuids) {
            TeamMember membre = new TeamMember();
            membre.setMemberId("id-" + p);
            membre.setRiotPuuid(p);
            m.put(p, membre);
        }
        return m;
    }

    @Test
    @DisplayName("une paire se juge contre l'attendu : la moyenne des taux de chacun sans l'autre")
    void duoAuDelaDeLAttendu() {
        List<RiotStatsGateway.SharedMatch> parties = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            parties.add(partie(i < 9, List.of("a", "b", "c", "d")));
        }
        for (int i = 0; i < 4; i++) {
            parties.add(partie(i < 2, List.of("a", "c", "d", "e")));
            parties.add(partie(i < 1, List.of("b", "c", "d", "e")));
        }

        List<TeamSynergyDto.Duo> duos = TeamSynergyService.duos(parties, effectif("a", "b", "c", "d", "e"), Map.of());
        TeamSynergyDto.Duo ab = duos.stream().filter(d -> d.memberA().equals("id-a") && d.memberB().equals("id-b"))
                .findFirst().orElseThrow();

        assertThat(ab.games()).isEqualTo(12);
        assertThat(ab.winRate()).isCloseTo(0.75, within(1e-9));
        assertThat(ab.expected()).isCloseTo((0.5 + 0.25) / 2, within(1e-9));
        assertThat(ab.delta()).isCloseTo(0.375, within(1e-9));
        assertThat(duos).noneMatch(d -> d.memberB().equals("id-e") && d.memberA().equals("id-a"));
    }

    @Test
    @DisplayName("les parts d'or et de dégâts d'un poste se lisent sur toute l'équipe, alliés hors effectif compris")
    void parts() {
        List<TeamSynergyDto.Resource> r = TeamSynergyService.ressources(List.of(partie(true, List.of("adc", "mid"))));
        TeamSynergyDto.Resource adc = r.stream().filter(l -> l.position().equals("BOTTOM")).findFirst().orElseThrow();

        assertThat(adc.goldShareInWins()).isCloseTo(0.5, within(1e-9));
        assertThat(adc.damageShareInWins()).isCloseTo(1 / 3.0, within(1e-9));
        assertThat(adc.conversion()).isNegative();
    }
}
