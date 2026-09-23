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
                6, 5, 9, 0, 0, 0, 0, 0, 25, 20, 0, 3600, null, null);

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
}
