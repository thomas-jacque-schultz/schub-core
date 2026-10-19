package schultz.thomas.schub.core.lol.seo.api.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import schultz.thomas.schub.core.api.client.riot.IndexablePlayerDto;
import schultz.thomas.schub.core.api.client.riot.SeoCreatorsDto;

import java.util.List;

// Délai long : remplacer la liste résout chaque nouveau Riot ID chez Riot, sous le quota de la collecte.
@Service
public class ConnectorRiotSeoGateway {

    private final RestClient restClient;

    public ConnectorRiotSeoGateway(@Qualifier("connectorRiotPurgeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public SeoCreatorsDto creators() {
        return restClient.get().uri("/seo/creators").retrieve().body(SeoCreatorsDto.class);
    }

    public SeoCreatorsDto replaceCreators(List<String> riotIds) {
        return restClient.put().uri("/seo/creators").body(riotIds).retrieve().body(SeoCreatorsDto.class);
    }

    public List<IndexablePlayerDto> indexable(int limit) {
        List<IndexablePlayerDto> joueurs = restClient.get()
                .uri(uri -> uri.path("/players/indexable").queryParam("limit", limit).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<IndexablePlayerDto>>() { });
        return joueurs == null ? List.of() : joueurs;
    }
}
