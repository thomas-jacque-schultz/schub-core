package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatLinesTest {

    @Test
    @DisplayName("KP et DP sont des parts de l'équipe, calculées sur des sommes")
    void partsDeLEquipe() {
        RiotStatsGateway.Bucket bucket = new RiotStatsGateway.Bucket("p", "", null, 2, 1,
                6, 5, 9, 0, 0, 0, 0, 0, 25, 20, 0, 3600, null, null, null);

        StatLineDto ligne = StatLines.of(bucket, "", null, null);

        assertThat(ligne.killParticipation()).isCloseTo(0.6, within(1e-9));
        assertThat(ligne.deathShare()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    @DisplayName("Sans total d'équipe, pas de part : un tiret, pas un zéro")
    void sansTotal() {
        StatLineDto ligne = StatLines.of(StatLines.vide("p"), "", null, null);

        assertThat(ligne.killParticipation()).isNull();
        assertThat(ligne.deathShare()).isNull();
    }

    @Test
    @DisplayName("les écarts à 15 min se divisent par les parties à timeline, pas par toutes les parties")
    void ecartsSurLesPartiesATimeline() {
        RiotStatsGateway.Bucket bucket = new RiotStatsGateway.Bucket("p", "", null, 10, 5, 0, 20, 0, 0, 0, 30000, 0, 0,
                0, 0, 0, 18000, null, null, new RiotStatsGateway.Performance(0, 30, 12, 1800, 0, 15, 0, 150000, 8, 16, 4,
                        2000, 40, 0, 2));

        StatLineDto ligne = StatLines.of(bucket, "", null, null);

        assertThat(ligne.laningGames()).isEqualTo(4);
        assertThat(ligne.goldDiffAt15()).isCloseTo(500, within(1e-9));
        assertThat(ligne.platesDiff()).isCloseTo(2, within(1e-9));
        assertThat(ligne.wardsKilledPerMinute()).isCloseTo(0.1, within(1e-9));
        assertThat(ligne.controlWardsPlaced()).isCloseTo(1.2, within(1e-9));
        assertThat(ligne.damageShare()).isCloseTo(0.2, within(1e-9));
        assertThat(ligne.deathsPer10()).isCloseTo(20 * 10.0 / 300, within(1e-9));
        assertThat(ligne.timeDeadShare()).isCloseTo(0.1, within(1e-9));
    }

    @Test
    @DisplayName("sans timeline, pas d'écart : un tiret, pas un zéro")
    void sansTimeline() {
        StatLineDto ligne = StatLines.of(StatLines.vide("p"), "", null, null);

        assertThat(ligne.goldDiffAt15()).isNull();
        assertThat(ligne.platesDiff()).isNull();
    }
}
