package schultz.thomas.schub.core.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
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
 * inexistant, clé de développement expirée — elle l'est toutes les 24 h : dans les trois cas la
 * bonne réponse est « je ne sais pas », et l'appelant décide quoi en faire. Le seul cas qu'on ne
 * veut pas est qu'une équipe devienne impossible à constituer, ou un Riot ID impossible à
 * déclarer, parce qu'un service tiers est indisponible.</p>
 *
 * <p><strong>Ce que cette classe ne distingue pas, et c'est un choix.</strong> Un 404 (« ce Riot
 * ID n'existe pas ») et un 503 (« le connecteur n'a pas de clé ») donnent le même
 * {@link Optional#empty()}. Les séparer demanderait de lire le statut HTTP ici et de le
 * transporter jusqu'aux deux appelants, pour une conduite identique des deux côtés : on accepte
 * la saisie, et le {@code puuid} sera rattrapé plus tard. Le jour où un écran devra dire « ce
 * pseudo n'existe pas » plutôt que « réessayez », c'est cette classe qu'il faudra rouvrir, et
 * elle seule.</p>
 */
@Slf4j
@Service
public class ConnectorRiotIdResolver implements RiotIdResolver {

    private final RestClient restClient;

    public ConnectorRiotIdResolver(@Qualifier("connectorRiotRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RiotIdResolution resolve(String gameName, String tagLine) {
        if (gameName == null || gameName.isBlank() || tagLine == null || tagLine.isBlank()) {
            return RiotIdResolution.unavailable();
        }
        try {
            PlayerIdentityResponse identity = restClient.get()
                    .uri(uri -> uri.path("/players")
                            .queryParam("gameName", gameName)
                            .queryParam("tagLine", tagLine)
                            .build())
                    .retrieve()
                    .body(PlayerIdentityResponse.class);
            String puuid = identity == null ? null : identity.puuid();
            return puuid == null ? RiotIdResolution.notFound() : RiotIdResolution.resolved(puuid);
        } catch (HttpClientErrorException.NotFound e) {
            return RiotIdResolution.notFound();
        } catch (RestClientException e) {
            log.warn("Riot ID {}#{} non résolu ({}) — connecteur indisponible",
                    gameName, tagLine, e.getMessage());
            return RiotIdResolution.unavailable();
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
