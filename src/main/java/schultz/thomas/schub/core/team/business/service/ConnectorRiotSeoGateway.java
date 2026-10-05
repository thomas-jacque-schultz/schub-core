package schultz.thomas.schub.core.team.business.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

// Délai long : remplacer la liste résout chaque nouveau Riot ID chez Riot, sous le quota de la collecte.
@Service
public class ConnectorRiotSeoGateway {

    private final RestClient restClient;

    public ConnectorRiotSeoGateway(@Qualifier("connectorRiotPurgeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public SeoCreators creators() {
        return restClient.get().uri("/seo/creators").retrieve().body(SeoCreators.class);
    }

    public SeoCreators replaceCreators(List<String> riotIds) {
        return restClient.put().uri("/seo/creators").body(riotIds).retrieve().body(SeoCreators.class);
    }

    public List<IndexablePlayer> indexable(int limit) {
        List<IndexablePlayer> joueurs = restClient.get()
                .uri(uri -> uri.path("/players/indexable").queryParam("limit", limit).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<IndexablePlayer>>() { });
        return joueurs == null ? List.of() : joueurs;
    }

    public record SeoCreators(List<String> creators, List<String> unknown) {
    }

    public record IndexablePlayer(String gameName, String tagLine, boolean featured) {

        public String riotId() {
            return gameName + "#" + tagLine;
        }
    }
}
