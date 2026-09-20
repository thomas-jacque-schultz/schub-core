package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Demande la résolution au connecteur Riot, qui détient la clé, le quota et le cache.
 *
 * <p>Le cœur ne parle pas à l'API Riot et ne doit pas l'apprendre : les limites de débit sont une
 * contrainte du système externe, donc leur place est dans le connecteur (migration §4, plan
 * §D.1). Ici, c'est un appel local sur l'overlay.</p>
 *
 * <p><strong>Toute erreur est avalée, volontairement.</strong> Connecteur éteint, Riot ID
 * inexistant, clé expirée : dans les trois cas la bonne réponse est « je ne sais pas », et le
 * membre est ajouté sans {@code puuid}. Le seul cas qu'on ne veut pas est qu'une équipe devienne
 * impossible à constituer parce qu'un service tiers est indisponible.</p>
 */
@Slf4j
@Service
public class ConnectorRiotIdResolver implements RiotIdResolver {

    private final RestClient restClient;

    public ConnectorRiotIdResolver(@Qualifier("connectorRiotRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<String> resolvePuuid(String gameName, String tagLine) {
        if (gameName == null || gameName.isBlank() || tagLine == null || tagLine.isBlank()) {
            return Optional.empty();
        }
        try {
            PlayerIdentityResponse identity = restClient.get()
                    .uri(uri -> uri.path("/players")
                            .queryParam("gameName", gameName)
                            .queryParam("tagLine", tagLine)
                            .build())
                    .retrieve()
                    .body(PlayerIdentityResponse.class);
            return Optional.ofNullable(identity).map(PlayerIdentityResponse::puuid);
        } catch (RestClientException e) {
            log.warn("Riot ID {}#{} non résolu ({}) — le membre est ajouté sans puuid",
                    gameName, tagLine, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * La forme rendue par le connecteur, redéclarée ici plutôt que partagée : deux services ne
     * partagent pas de classes, c'est ce qui leur permet d'évoluer séparément (migration §5).
     * Seul {@code puuid} est lu — le reste est ignoré à la désérialisation.
     */
    record PlayerIdentityResponse(String puuid, String gameName, String tagLine) {
    }
}
