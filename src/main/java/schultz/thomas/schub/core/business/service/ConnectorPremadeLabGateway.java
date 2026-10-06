package schultz.thomas.schub.core.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

// Les messages privés de l'application Discord PremadeLab (Schub#100).
@Slf4j
@Service
public class ConnectorPremadeLabGateway {

    private final RestClient restClient;

    public ConnectorPremadeLabGateway(@Qualifier("connectorPremadeLabRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Status sendDirectMessage(String discordId, String content) {
        try {
            Result result = restClient.post().uri("/dm")
                    .body(Map.of("discordId", discordId, "content", content))
                    .retrieve().body(Result.class);
            return result == null || result.status() == null ? Status.FAILED : result.status();
        } catch (RestClientException indisponible) {
            log.warn("Connecteur PremadeLab injoignable : {}", indisponible.getMessage());
            return Status.FAILED;
        }
    }

    public enum Status {
        SENT,
        UNREACHABLE,
        DISABLED,
        FAILED
    }

    record Result(Status status, String detail) {
    }
}
