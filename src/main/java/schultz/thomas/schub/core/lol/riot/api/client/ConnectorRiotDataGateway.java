package schultz.thomas.schub.core.lol.riot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.api.client.riot.IngestPauseRequest;
import schultz.thomas.schub.core.api.client.riot.IngestPauseStatusDto;
import schultz.thomas.schub.core.api.client.riot.RiotDataInventoryDto;
import schultz.thomas.schub.core.api.client.riot.RiotDataPurgeReportDto;
import schultz.thomas.schub.core.lol.riot.business.service.RiotDataGateway;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ConnectorRiotDataGateway implements RiotDataGateway {

    private final RestClient restClient;

    private final RiotContractMapper contrat;

    public ConnectorRiotDataGateway(@Qualifier("connectorRiotPurgeRestClient") RestClient restClient,
                                    RiotContractMapper contrat) {
        this.restClient = restClient;
        this.contrat = contrat;
    }

    @Override
    public Optional<IngestPause> pause() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest/pause").retrieve().body(IngestPauseStatusDto.class))
                    .map(contrat::toPause);
        } catch (RestClientException indisponible) {
            log.debug("Pause de l'ingest indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public IngestPause setPause(boolean paused) {
        return contrat.toPause(restClient.put().uri("/ingest/pause").body(new IngestPauseRequest().paused(paused))
                .retrieve().body(IngestPauseStatusDto.class));
    }

    @Override
    public Optional<Inventory> inventory() {
        try {
            return Optional.ofNullable(restClient.get().uri("/riot-data").retrieve().body(RiotDataInventoryDto.class))
                    .map(contrat::toInventory);
        } catch (RestClientException indisponible) {
            log.debug("Inventaire des données Riot indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Long> purge() {
        try {
            RiotDataPurgeReportDto reponse = restClient.delete().uri("/riot-data").retrieve()
                    .body(RiotDataPurgeReportDto.class);
            return reponse == null || reponse.getRemoved() == null ? Map.of() : reponse.getRemoved();
        } catch (HttpClientErrorException.Conflict pasALArret) {
            ProblemDetail detail = pasALArret.getResponseBodyAs(ProblemDetail.class);
            throw new IllegalStateException(detail == null || detail.getDetail() == null
                    ? "L'ingest doit être en pause avant l'invalidation." : detail.getDetail());
        }
    }
}
