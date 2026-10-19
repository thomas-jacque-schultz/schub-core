package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.service.ConnectorPremadeLabGateway.Embed;

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
}
