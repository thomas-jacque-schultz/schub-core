package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import schultz.thomas.schub.core.team.api.dto.EarlyGameDto;
import schultz.thomas.schub.core.team.api.dto.TeamEarlyGameDto;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyGamesTest {

    private static final Instant QUAND = Instant.parse("2026-09-20T20:00:00Z");

    private static TeamMember membre(String id, String puuid) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(id);
        membre.setRiotPuuid(puuid);
        return membre;
    }

    private static final Map<String, TeamMember> PAR_PUUID = Map.of(
            "top", membre("m-top", "top"), "jgl", membre("m-jgl", "jgl"));

    // Nous en bleu : top et jungler membres. Leur jungler vient deux fois en haut, le nôtre une fois en bas.
    private static RiotStatsGateway.EarlyGame early() {
        return new RiotStatsGateway.EarlyGame(
                List.of(
                        new RiotStatsGateway.Gank(200, "TOP", 200, "jgl-r", List.of("top"), "KILL", 1, 0,
                                List.of("top"), false, true),
                        new RiotStatsGateway.Gank(400, "TOP", 200, "jgl-r", List.of("top"), "SURVIVED", 0, 0,
                                List.of(), false, false),
                        new RiotStatsGateway.Gank(500, "BOT", 100, "jgl", List.of("bot-r", "sup-r"), "COUNTER", 0, 1,
                                List.of("jgl"), false, false)),
                List.of(new RiotStatsGateway.JunglePresence("jgl", 100, 2, 1, 10),
                        new RiotStatsGateway.JunglePresence("jgl-r", 200, 9, 1, 3)),
                List.of(new RiotStatsGateway.Objectives(100, 1, 0, 0), new RiotStatsGateway.Objectives(200, 0, 3, 1)));
    }

    private static RiotStatsGateway.SharedMatch partie(boolean win) {
        return new RiotStatsGateway.SharedMatch("g1", QUAND, 1800, 420, "RANKED_SOLO", "16.18", 4, false, win,
                List.of(new RiotStatsGateway.SharedMatchPlayer("top", 1, "A", "TOP", win, 100, 0, 0, 0, 0, 0, 0,
                        0, 0, false)), List.of());
    }

    private static RiotStatsGateway.Insight insight() {
        return new RiotStatsGateway.Insight("g1", true, null, early(), List.of(
                new RiotStatsGateway.InsightPlayer("top", 100, "TOP", 1, null, null, null),
                new RiotStatsGateway.InsightPlayer("jgl", 100, "JUNGLE", 2, null, null, null)));
    }

    @Test
    @DisplayName("Vue d'une partie : chaque gank dit depuis notre camp, pertes comprises")
    void vue() {
        EarlyGameDto vue = EarlyGames.vue(early(), 100, PAR_PUUID);

        assertThat(vue.ganks()).extracting(EarlyGameDto.GankDto::ours).containsExactly(false, false, true);
        EarlyGameDto.GankDto subi = vue.ganks().getFirst();
        assertThat(subi.targetMemberIds()).containsExactly("m-top");
        assertThat(subi.fallenMemberIds()).containsExactly("m-top");
        assertThat(subi.alliesLost()).isEqualTo(1);
        assertThat(subi.enemiesLost()).isZero();
        EarlyGameDto.GankDto fait = vue.ganks().getLast();
        assertThat(fait.alliesLost()).isEqualTo(1);
        assertThat(fait.enemiesLost()).isZero();
        assertThat(vue.ourJungler().memberId()).isEqualTo("m-jgl");
        assertThat(vue.ourJungler().strongSide()).isEqualTo("BOT");
        assertThat(vue.theirJungler().strongSide()).isEqualTo("TOP");
        assertThat(vue.theirObjectives().grubs()).isEqualTo(3);
    }

    @Test
    @DisplayName("Bilan : le top a encaissé un gank sur deux, le jungler n'a rien fait de décisif")
    void bilan() {
        TeamEarlyGameDto bilan = EarlyGames.bilan(List.of(partie(true)), Map.of("g1", insight()), PAR_PUUID,
                Map.of("m-top", "Top", "m-jgl", "Jungle"));

        assertThat(bilan.games()).isEqualTo(1);
        TeamEarlyGameDto.MemberEarlyDto top = bilan.members().stream()
                .filter(m -> m.memberId().equals("m-top")).findFirst().orElseThrow();
        assertThat(top.ganksFaced()).isEqualTo(2);
        assertThat(top.ganksHeld()).isEqualTo(1);
        assertThat(top.deathsOnGank()).isEqualTo(1);
        TeamEarlyGameDto.MemberEarlyDto jungler = bilan.members().stream()
                .filter(m -> m.memberId().equals("m-jgl")).findFirst().orElseThrow();
        assertThat(jungler.ganksMade()).isEqualTo(1);
        assertThat(jungler.ganksDecisive()).isZero();
        assertThat(jungler.ganksCountered()).isEqualTo(1);
        assertThat(jungler.botMinutes()).isEqualTo(10);

        assertThat(bilan.strongSides()).singleElement().satisfies(cote -> {
            assertThat(cote.side()).isEqualTo("BOT");
            assertThat(cote.wins()).isEqualTo(1);
            assertThat(cote.enemyGanks()).isEqualTo(2);
            assertThat(cote.enemyGanksOnWeakSide()).isEqualTo(2);
        });
    }
}
