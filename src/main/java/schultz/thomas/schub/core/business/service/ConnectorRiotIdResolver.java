package schultz.thomas.schub.core.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

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
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.info("Riot ID {}#{} non résolu — connecteur occupé, à réessayer{}",
                    gameName, tagLine, retryAfter(e));
            return RiotIdResolution.busy();
        } catch (RestClientException e) {
            log.warn("Riot ID {}#{} non résolu ({}) — connecteur indisponible",
                    gameName, tagLine, e.getMessage());
            return RiotIdResolution.unavailable();
        }
    }

    private String retryAfter(HttpClientErrorException e) {
        String delai = e.getResponseHeaders() == null ? null
                : e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        return delai == null ? "" : " dans " + delai + " s";
    }

    record PlayerIdentityResponse(String puuid, String gameName, String tagLine) {
    }
}
