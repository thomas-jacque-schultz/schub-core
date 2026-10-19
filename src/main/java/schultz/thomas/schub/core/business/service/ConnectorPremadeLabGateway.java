package schultz.thomas.schub.core.business.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

// Les messages privés de l'application Discord PremadeLab (Schub#100).
@Slf4j
@Service
public class ConnectorPremadeLabGateway {

    private final RestClient restClient;

    public ConnectorPremadeLabGateway(@Qualifier("connectorPremadeLabRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Result sendDirectMessage(String discordId, List<Embed> embeds) {
        try {
            Result result = restClient.post().uri("/dm")
                    .body(Map.of("discordId", discordId, "embeds", embeds))
                    .retrieve().body(Result.class);
            return result == null || result.status() == null ? new Result(Status.FAILED, "Réponse vide") : result;
        } catch (RestClientException indisponible) {
            log.warn("Connecteur PremadeLab injoignable : {}", indisponible.getMessage());
            return new Result(Status.FAILED, "Connecteur PremadeLab injoignable");
        }
    }

    public enum Status {
        SENT,
        UNREACHABLE,
        DISABLED,
        FAILED
    }

    public record Result(Status status, String detail) {
    }

    // Le format des encadrés de l’API Discord, relayé tel quel par le connecteur.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Embed(String title, String url, String description, Integer color, List<Field> fields,
                        Footer footer) {

        public record Field(String name, String value, boolean inline) {
        }

        public record Footer(String text) {
        }
    }
}
