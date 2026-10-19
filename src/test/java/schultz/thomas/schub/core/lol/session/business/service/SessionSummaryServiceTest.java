package schultz.thomas.schub.core.lol.session.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.service.PremadeLabGateway.Embed;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.riot.business.service.VueDePartie;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionSummaryServiceTest {

    @Test
    @DisplayName("un écart est signé, en milliers au-delà de 1 000")
    void signe() {
        assertThat(SessionSummaryService.signe(820)).isEqualTo("+820");
        assertThat(SessionSummaryService.signe(-14)).isEqualTo("−14");
        assertThat(SessionSummaryService.signe(2140)).isEqualTo("+2,1 k");
        assertThat(SessionSummaryService.signe(0)).isEqualTo("+0");
    }

    @Test
    @DisplayName("une durée se dit en minutes, puis en heures")
    void duree() {
        assertThat(SessionSummaryService.duree(31 * 60 + 40)).isEqualTo("31 min");
        assertThat(SessionSummaryService.duree(98 * 60)).isEqualTo("1 h 38");
    }

    @Test
    @DisplayName("un message garde 10 encadrés et 6 000 caractères au plus, et jamais moins d'un")
    void limite() {
        Embed court = new Embed("Session", null, "x".repeat(100), null, List.of(), null);
        Embed long_ = new Embed("Session", null, "x".repeat(4000), null, List.of(), null);

        assertThat(SessionSummaryService.limite(Collections.nCopies(12, court))).hasSize(10);
        assertThat(SessionSummaryService.limite(List.of(long_, long_))).hasSize(1);
        assertThat(SessionSummaryService.limite(List.of(court))).hasSize(1);
    }

    @Test
    @DisplayName("un nom Riot perd son tag, un nom choisi reste tel quel")
    void sansTag() {
        assertThat(SessionSummaryService.sansTag("MagicieN972#EUW")).isEqualTo("MagicieN972");
        assertThat(SessionSummaryService.sansTag("Pikatchu ♘")).isEqualTo("Pikatchu ♘");
        assertThat(SessionSummaryService.sansTag(null)).isNull();
    }

    @Test
    @DisplayName("l'écart final compte l'or des dix joueurs, depuis notre camp, et seulement à cinq contre cinq")
    void ecartFinal() {
        RiotStatsGateway.SharedMatch p = partie("RANKED_SOLO",
                List.of(joueur("moi", 100, "TOP", 12000), joueur("ami", 100, "MIDDLE", 11000)),
                List.of(joueur("a1", 100, "JUNGLE", 9000), joueur("a2", 100, "BOTTOM", 10000),
                        joueur("a3", 100, "UTILITY", 7000), joueur("e1", 200, "TOP", 10000),
                        joueur("e2", 200, "JUNGLE", 9000), joueur("e3", 200, "MIDDLE", 12000),
                        joueur("e4", 200, "BOTTOM", 10000), joueur("e5", 200, "UTILITY", 4000),
                        joueur("e5", 200, "UTILITY", 4000)));

        assertThat(VueDePartie.de(p).ecartDOrFinal()).isEqualTo(4000);
        assertThat(VueDePartie.de(partie("OTHER",
                List.of(joueur("moi", 100, "UNKNOWN", 1)), List.of(joueur("eux", 200, "UNKNOWN", 1))))
                .ecartDOrFinal())
                .isNull();
        assertThat(VueDePartie.de(partie("ARAM", List.of(joueur("moi", 100, "", 1)), List.of())).ecartDOrFinal())
                .isNull();
    }

    @Test
    @DisplayName("les postes joués, du plus fréquent au moins fréquent, un mode hors Faille compté à part")
    void postes() {
        List<RiotStatsGateway.SharedMatch> parties = List.of(
                partie("RANKED_SOLO", List.of(joueur("moi", 100, "MIDDLE", 0)), List.of()),
                partie("RANKED_SOLO", List.of(joueur("moi", 100, "TOP", 0)), List.of()),
                partie("RANKED_SOLO", List.of(joueur("moi", 100, "TOP", 0)), List.of()),
                partie("ARAM", List.of(joueur("moi", 100, "", 0)), List.of()),
                partie("OTHER", List.of(joueur("moi", 100, "UNKNOWN", 0)), List.of()),
                partie("RANKED_SOLO", List.of(joueur("ami", 100, "BOTTOM", 0)), List.of()));

        assertThat(SessionSummaryService.postes(parties, "moi"))
                .isEqualTo("Top ×2, Mid ×1, ARAM ×1, autre mode ×1");
    }

    private static RiotStatsGateway.SharedMatchPlayer joueur(String puuid, int side, String poste, int or) {
        return new RiotStatsGateway.SharedMatchPlayer(puuid, 1, "Gnar", poste, side == 100, side, 0, 0, 0, 0, or,
                0, 0, 0, false, puuid);
    }

    private static RiotStatsGateway.SharedMatch partie(String queue, List<RiotStatsGateway.SharedMatchPlayer> nous,
                                                       List<RiotStatsGateway.SharedMatchPlayer> autres) {
        return new RiotStatsGateway.SharedMatch("EUW1_1", Instant.EPOCH, 1800, 420, queue, "15.1", nous.size(),
                false, true, nous, autres);
    }
}
