package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
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

    // Lecture de la table du jour : vide si le connecteur ne l'a pas encore calculée, ou ne répond pas.
    public Optional<TierCombos> tierCombos(String tier, String kind, int minGames, int limit) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uri -> uri.path("/stats/tier-combos").queryParam("tier", tier).queryParam("kind", kind)
                            .queryParam("minGames", minGames).queryParam("limit", limit).build())
                    .retrieve()
                    .body(TierCombos.class));
        } catch (RestClientException indisponible) {
            log.warn("Combos du palier {} ({}) indisponibles : {}", tier, kind, indisponible.getMessage());
            return Optional.empty();
        }
    }

    public record TierCombos(String tier, String kind, Instant computedAt, long games, List<Ranked> combos) {
    }

    public record Ranked(List<Integer> championIds, long games, long wins, double lowerBound) {
    }

    // position : celle de Riot (TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY).
    public record Slot(String position, List<Integer> championIds) {
    }

    public record GeneralCombo(List<Integer> championIds, long games, long wins) {
    }
}
