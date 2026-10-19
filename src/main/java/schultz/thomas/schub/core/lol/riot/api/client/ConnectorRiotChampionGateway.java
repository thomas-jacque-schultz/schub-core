package schultz.thomas.schub.core.lol.riot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.api.client.riot.ChampionCardDto;
import schultz.thomas.schub.core.api.client.riot.ChampionCatalogDto;
import schultz.thomas.schub.core.api.client.riot.ChampionMasteryDto;
import schultz.thomas.schub.core.lol.riot.business.service.RiotChampionGateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ConnectorRiotChampionGateway implements RiotChampionGateway {

    private static final ParameterizedTypeReference<List<ChampionMasteryDto>> MAITRISES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RiotContractMapper contrat;

    public ConnectorRiotChampionGateway(@Qualifier("connectorRiotRestClient") RestClient restClient,
                                        RiotContractMapper contrat) {
        this.restClient = restClient;
        this.contrat = contrat;
    }

    @Override
    public Optional<Catalogue> catalogue() {
        try {
            ChampionCatalogDto reponse = restClient.get()
                    .uri("/champions")
                    .retrieve()
                    .body(ChampionCatalogDto.class);
            if (reponse == null || reponse.getVersion() == null || reponse.getChampions() == null) {
                log.warn("Catalogue des champions vide ou sans version — traité comme indisponible");
                return Optional.empty();
            }
            return Optional.of(new Catalogue(reponse.getVersion(), indexe(reponse.getChampions())));
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
            List<ChampionMasteryDto> reponse = restClient.get()
                    .uri(uri -> uri.path("/players/{puuid}/champion-mastery")
                            .queryParamIfPresent("limit",
                                    limit > 0 ? java.util.Optional.of(limit) : java.util.Optional.empty())
                            .build(puuid))
                    .retrieve()
                    .body(MAITRISES);
            if (reponse == null) {
                return Optional.empty();
            }
            return Optional.of(reponse.stream().map(contrat::toMastery).toList());
        } catch (RestClientException e) {
            log.warn("Maîtrises non obtenues pour un joueur ({}) — sa colonne dira pourquoi elle est vide",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private Map<Integer, Champion> indexe(List<ChampionCardDto> champions) {
        Map<Integer, Champion> parId = new LinkedHashMap<>();
        for (ChampionCardDto carte : champions) {
            if (carte != null) {
                Champion champion = contrat.toChampion(carte);
                parId.put(champion.id(), champion);
            }
        }
        return Map.copyOf(parId);
    }
}
