package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ConnectorRiotDataGateway implements RiotDataGateway {

    private final RestClient restClient;

    public ConnectorRiotDataGateway(@Qualifier("connectorRiotPurgeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<IngestPause> pause() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest/pause").retrieve().body(IngestPause.class));
        } catch (RestClientException indisponible) {
            log.debug("Pause de l'ingest indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public IngestPause setPause(boolean paused) {
        return restClient.put().uri("/ingest/pause").body(Map.of("paused", paused)).retrieve().body(IngestPause.class);
    }

    @Override
    public Optional<Inventory> inventory() {
        try {
            return Optional.ofNullable(restClient.get().uri("/riot-data").retrieve().body(Inventory.class));
        } catch (RestClientException indisponible) {
            log.debug("Inventaire des données Riot indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Long> purge() {
        try {
            PurgeResponse reponse = restClient.delete().uri("/riot-data").retrieve().body(PurgeResponse.class);
            return reponse == null || reponse.removed() == null ? Map.of() : reponse.removed();
        } catch (HttpClientErrorException.Conflict pasALArret) {
            ProblemDetail detail = pasALArret.getResponseBodyAs(ProblemDetail.class);
            throw new IllegalStateException(detail == null || detail.getDetail() == null
                    ? "L'ingest doit être en pause avant l'invalidation." : detail.getDetail());
        }
    }

    private record PurgeResponse(Map<String, Long> removed) {
    }
}
