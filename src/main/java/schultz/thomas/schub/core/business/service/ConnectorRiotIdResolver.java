package schultz.thomas.schub.core.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
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

    private static final Duration RECENTE = Duration.ofHours(1);

    @Override
    public RiotIdResolution resolve(String gameName, String tagLine) {
        return resolve(gameName, tagLine, null);
    }

    @Override
    public RiotIdResolution resolveRecent(String gameName, String tagLine) {
        return resolve(gameName, tagLine, RECENTE);
    }

    private RiotIdResolution resolve(String gameName, String tagLine, Duration maxAge) {
        if (gameName == null || gameName.isBlank() || tagLine == null || tagLine.isBlank()) {
            return RiotIdResolution.unavailable();
        }
        try {
            PlayerIdentityResponse identity = restClient.get()
                    .uri(uri -> uri.path("/players")
                            .queryParam("gameName", gameName)
                            .queryParam("tagLine", tagLine)
                            .queryParamIfPresent("maxAge", java.util.Optional.ofNullable(maxAge))
                            .build())
                    .retrieve()
                    .body(PlayerIdentityResponse.class);
            String puuid = identity == null ? null : identity.puuid();
            return puuid == null ? RiotIdResolution.notFound()
                    : RiotIdResolution.resolved(puuid, identity.gameName(), identity.tagLine());
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
