package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Délai long : le repère général parcourt les participations de tous les joueurs collectés.
@Slf4j
@Service
public class ConnectorRiotComboGateway {

    private final RestClient restClient;

    public ConnectorRiotComboGateway(@Qualifier("connectorRiotPurgeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<List<GeneralCombo>> combos(List<Slot> slots, int minGames) {
        try {
            return Optional.ofNullable(restClient.post().uri("/stats/champion-combos")
                    .body(Map.of("slots", slots, "minGames", minGames))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GeneralCombo>>() { }));
        } catch (RestClientException indisponible) {
            log.warn("Repère général des combos indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    // position : celle de Riot (TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY).
    public record Slot(String position, List<Integer> championIds) {
    }

    public record GeneralCombo(List<Integer> championIds, long games, long wins) {
    }
}
