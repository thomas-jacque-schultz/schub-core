package schultz.thomas.schub.core.api.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.business.service.PremadeLabGateway;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConnectorPremadeLabGateway implements PremadeLabGateway {

    private final RestClient restClient;

    public ConnectorPremadeLabGateway(@Qualifier("connectorPremadeLabRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Result sendDirectMessage(String discordId, List<Embed> embeds) {
        try {
            Result result = restClient.post().uri("/dm")
                    .body(Map.of("discordId", discordId, "embeds", embeds.stream().map(EmbedJson::de).toList()))
                    .retrieve().body(Result.class);
            return result == null || result.status() == null ? new Result(Status.FAILED, "Réponse vide") : result;
        } catch (RestClientException indisponible) {
            log.warn("Connecteur PremadeLab injoignable : {}", indisponible.getMessage());
            return new Result(Status.FAILED, "Connecteur PremadeLab injoignable");
        }
    }

    @Override
    public Map<String, String> championEmojis(List<ChampionIcon> champions) {
        if (champions.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, String> emojis = restClient.post().uri("/emojis/champions")
                    .body(Map.of("champions", champions))
                    .retrieve().body(EMOJIS);
            return emojis == null ? Map.of() : emojis;
        } catch (RestClientException indisponible) {
            log.warn("Emojis de champion non obtenus : {}", indisponible.getMessage());
            return Map.of();
        }
    }

    private static final ParameterizedTypeReference<Map<String, String>> EMOJIS = new ParameterizedTypeReference<>() {
    };

    // Relayé tel quel par le connecteur à l'API Discord ; les champs absents ne partent pas.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record EmbedJson(String title, String url, String description, Integer color, List<Embed.Field> fields,
                             Embed.Footer footer) {

        static EmbedJson de(Embed embed) {
            return new EmbedJson(embed.title(), embed.url(), embed.description(), embed.color(), embed.fields(),
                    embed.footer());
        }
    }
}
