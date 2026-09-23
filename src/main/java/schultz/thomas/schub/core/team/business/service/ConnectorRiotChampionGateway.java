package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ConnectorRiotChampionGateway implements RiotChampionGateway {

    private static final ParameterizedTypeReference<List<MasteryResponse>> MAITRISES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public ConnectorRiotChampionGateway(@Qualifier("connectorRiotRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Catalogue> catalogue() {
        try {
            CatalogueResponse reponse = restClient.get()
                    .uri("/champions")
                    .retrieve()
                    .body(CatalogueResponse.class);
            if (reponse == null || reponse.version() == null || reponse.champions() == null) {
                log.warn("Catalogue des champions vide ou sans version — traité comme indisponible");
                return Optional.empty();
            }
            return Optional.of(new Catalogue(reponse.version(), indexe(reponse.champions())));
        } catch (RestClientException e) {
            log.warn("Catalogue des champions non obtenu ({}) — le pool sera servi sans champions",
                    e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Mastery>> masteries(String puuid, int limit) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.empty();
        }
        try {
            List<MasteryResponse> reponse = restClient.get()
                    .uri(uri -> uri.path("/players/{puuid}/champion-mastery")
                            .queryParamIfPresent("limit",
                                    limit > 0 ? java.util.Optional.of(limit) : java.util.Optional.empty())
                            .build(puuid))
                    .retrieve()
                    .body(MAITRISES);
            if (reponse == null) {
                return Optional.empty();
            }
            return Optional.of(reponse.stream()
                    .map(m -> new Mastery(m.championId(), m.level(), m.points(),
                            m.lastPlayedAt(), m.observedAt()))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Maîtrises non obtenues pour un joueur ({}) — sa colonne dira pourquoi elle est vide",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private static Map<Integer, Champion> indexe(List<ChampionResponse> champions) {
        Map<Integer, Champion> parId = new LinkedHashMap<>();
        for (ChampionResponse champion : champions) {
            if (champion != null) {
                parId.put(champion.id(),
                        new Champion(champion.id(), champion.key(), champion.name(), champion.iconUrl()));
            }
        }
        return Map.copyOf(parId);
    }

    record CatalogueResponse(String version, String locale, List<ChampionResponse> champions) {
    }

    record ChampionResponse(String key, int id, String name, String iconUrl) {
    }

    record MasteryResponse(int championId, String championName, int level, int points,
                           Instant lastPlayedAt, Instant observedAt) {
    }
}
