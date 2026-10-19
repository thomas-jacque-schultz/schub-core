package schultz.thomas.schub.core.lol.riot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.api.client.riot.ChampionComboDto;
import schultz.thomas.schub.core.api.client.riot.ChampionCombosQueryDto;
import schultz.thomas.schub.core.api.client.riot.TierCombosDto;
import schultz.thomas.schub.core.lol.riot.business.service.RiotComboGateway;

import java.util.List;
import java.util.Optional;

// Délai long : le repère général parcourt les participations de tous les joueurs collectés.
@Slf4j
@Service
public class ConnectorRiotComboGateway implements RiotComboGateway {

    private static final ParameterizedTypeReference<List<ChampionComboDto>> COMBOS =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RiotContractMapper contrat;

    public ConnectorRiotComboGateway(@Qualifier("connectorRiotPurgeRestClient") RestClient restClient,
                                     RiotContractMapper contrat) {
        this.restClient = restClient;
        this.contrat = contrat;
    }

    @Override
    public Optional<List<GeneralCombo>> combos(List<Slot> slots, int minGames) {
        try {
            return Optional.ofNullable(restClient.post().uri("/stats/champion-combos")
                    .body(new ChampionCombosQueryDto().minGames(minGames)
                            .slots(slots.stream().map(contrat::toSlot).toList()))
                    .retrieve()
                    .body(COMBOS))
                    .map(lus -> lus.stream().map(contrat::toCombo).toList());
        } catch (RestClientException indisponible) {
            log.warn("Repère général des combos indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    // Lecture de la table du jour : vide si le connecteur ne l'a pas encore calculée, ou ne répond pas.
    @Override
    public Optional<TierCombos> tierCombos(String tier, String kind, int minGames, int limit) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uri -> uri.path("/stats/tier-combos").queryParam("tier", tier).queryParam("kind", kind)
                            .queryParam("minGames", minGames).queryParam("limit", limit).build())
                    .retrieve()
                    .body(TierCombosDto.class))
                    .map(contrat::toTierCombos);
        } catch (RestClientException indisponible) {
            log.warn("Combos du palier {} ({}) indisponibles : {}", tier, kind, indisponible.getMessage());
            return Optional.empty();
        }
    }
}
