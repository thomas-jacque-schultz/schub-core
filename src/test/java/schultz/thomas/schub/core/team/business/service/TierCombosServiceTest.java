package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.team.api.dto.TierCombosDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TierCombosServiceTest {

    private final ConnectorRiotComboGateway combos = mock(ConnectorRiotComboGateway.class);
    private final RiotChampionGateway champions = mock(RiotChampionGateway.class);
    private final TierCombosService service = new TierCombosService(combos, champions);

    @Test
    @DisplayName("chaque ligne nomme ses champions et porte le logo sous le seuil des conseils de draft")
    void lignesNommees() {
        when(combos.tierCombos("SILVER_GOLD", "ADC_SUP", TierCombosService.PARTIES_MINIMUM, TierCombosService.LIGNES))
                .thenReturn(Optional.of(new ConnectorRiotComboGateway.TierCombos("SILVER_GOLD", "ADC_SUP",
                        Instant.now(), 1200, List.of(
                        new ConnectorRiotComboGateway.Ranked(List.of(22, 412), 40, 26, 0.5),
                        new ConnectorRiotComboGateway.Ranked(List.of(51, 89), 3, 3, 0.43)))));
        when(champions.catalogue()).thenReturn(Optional.of(new RiotChampionGateway.Catalogue("16.20", Map.of(
                22, new RiotChampionGateway.Champion(22, "Ashe", "Ashe", "ashe.png"),
                412, new RiotChampionGateway.Champion(412, "Thresh", "Thresh", "thresh.png")))));

        TierCombosDto dto = service.of("SILVER_GOLD", "ADC_SUP");

        assertThat(dto.positions()).containsExactly("BOTTOM", "UTILITY");
        assertThat(dto.combos().get(0).champions()).extracting(TierCombosDto.Champion::name)
                .containsExactly("Ashe", "Thresh");
        assertThat(dto.combos().get(0).lowConfidence()).isFalse();
        assertThat(dto.combos().get(1).lowConfidence()).isTrue();
        assertThat(dto.combos().get(1).champions().get(0).name()).isNull();
    }

    @Test
    @DisplayName("un palier ou un type inconnu n'a pas de page")
    void pageInconnue() {
        assertThatThrownBy(() -> service.of("BRONZE", "ADC_SUP")).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.of("DIAMOND", "JGL_TOP")).isInstanceOf(NoSuchElementException.class);
    }
}
