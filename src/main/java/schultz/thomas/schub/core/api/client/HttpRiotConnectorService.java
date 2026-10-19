package schultz.thomas.schub.core.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.api.client.riot.AccountsByRankDto;
import schultz.thomas.schub.core.api.client.riot.CrawlerStatusDto;
import schultz.thomas.schub.core.api.client.riot.CrawlerToggleRequest;
import schultz.thomas.schub.core.api.client.riot.HistoryWindowDto;
import schultz.thomas.schub.core.api.client.riot.IngestStatusDto;
import schultz.thomas.schub.core.api.client.riot.IngestSummaryDto;
import schultz.thomas.schub.core.api.client.riot.PlayerIngestStatusDto;
import schultz.thomas.schub.core.api.client.riot.PlayerSuggestionDto;
import schultz.thomas.schub.core.api.client.riot.PuuidListRequest;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class HttpRiotConnectorService implements RiotConnectorService {

    private final RestClient restClient;

    private final RiotIngestContractMapper contrat;

    public HttpRiotConnectorService(@Qualifier("connectorRiotRestClient") RestClient restClient,
                                    RiotIngestContractMapper contrat) {
        this.restClient = restClient;
        this.contrat = contrat;
    }

    @Override
    public Optional<PlayerIngest> ingestOf(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/ingest/players/{puuid}", puuid)
                    .retrieve()
                    .body(PlayerIngestStatusDto.class))
                    .map(contrat::toIngest);
        } catch (RestClientException indisponible) {
            log.debug("État d'ingestion indisponible pour {} : {}", puuid, indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<IngestLoad> load() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest").retrieve().body(IngestStatusDto.class))
                    .map(contrat::toLoad);
        } catch (RestClientException indisponible) {
            log.debug("Charge de la collecte indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<IngestSummary> summary() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest/summary").retrieve().body(IngestSummaryDto.class))
                    .map(contrat::toSummary);
        } catch (RestClientException indisponible) {
            log.debug("Bilan de la collecte indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Crawler> crawler() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest/crawler").retrieve().body(CrawlerStatusDto.class))
                    .map(contrat::toCrawler);
        } catch (RestClientException indisponible) {
            log.debug("Collecte de fond indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<AccountsByRank> accountsByRank() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest/accounts-by-rank").retrieve()
                    .body(AccountsByRankDto.class))
                    .map(contrat::toAccounts);
        } catch (RestClientException indisponible) {
            log.debug("Comptes par rang indisponibles : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Crawler toggleCrawler(boolean enabled) {
        try {
            return contrat.toCrawler(restClient.put().uri("/ingest/crawler")
                    .body(new CrawlerToggleRequest().enabled(enabled))
                    .retrieve().body(CrawlerStatusDto.class));
        } catch (HttpClientErrorException.Conflict verrouillee) {
            throw new IllegalStateException("La collecte de fond ne se bascule pas dans cet environnement");
        } catch (RestClientException indisponible) {
            throw new RiotConnectorUnavailableException();
        }
    }

    @Override
    public List<String> stalePuuids(Instant since) {
        try {
            List<String> perimes = restClient.get()
                    .uri(uri -> uri.path("/puuids/stale").queryParam("since", since).build())
                    .retrieve().body(new ParameterizedTypeReference<List<String>>() { });
            return perimes == null ? List.of() : perimes;
        } catch (RestClientException indisponible) {
            log.debug("Puuid périmés indisponibles : {}", indisponible.getMessage());
            return List.of();
        }
    }

    @Override
    public List<String> checkPuuids(java.util.Collection<String> puuids) {
        if (puuids.isEmpty()) {
            return List.of();
        }
        try {
            List<String> perimes = restClient.post().uri("/puuids/check")
                    .body(new PuuidListRequest().puuids(List.copyOf(puuids)))
                    .retrieve().body(new ParameterizedTypeReference<List<String>>() { });
            return perimes == null ? List.of() : perimes;
        } catch (RestClientException indisponible) {
            log.warn("Vérification des puuid impossible : {}", indisponible.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<HistoryWindow> historyWindow() {
        try {
            return Optional.ofNullable(restClient.get().uri("/ingest/history-window").retrieve()
                    .body(HistoryWindowDto.class))
                    .map(contrat::toWindow);
        } catch (RestClientException indisponible) {
            log.debug("Fenêtre de relevé indisponible : {}", indisponible.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public HistoryWindow updateHistoryWindow(HistoryWindow window) {
        try {
            return contrat.toWindow(restClient.put().uri("/ingest/history-window").body(contrat.toDto(window))
                    .retrieve().body(HistoryWindowDto.class));
        } catch (HttpClientErrorException.BadRequest horsBornes) {
            throw new IllegalArgumentException("Fenêtre de relevé hors bornes");
        } catch (RestClientException indisponible) {
            throw new RiotConnectorUnavailableException();
        }
    }

    @Override
    public boolean requestIngest(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return false;
        }
        try {
            restClient.post().uri("/players/{puuid}/matches/sync", puuid).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException indisponible) {
            log.warn("Collecte non demandée pour {} ({}) — elle le sera à la prochaine occasion",
                    puuid, indisponible.getMessage());
            return false;
        }
    }

    @Override
    public boolean requestBackfill(String puuid) {
        try {
            restClient.post().uri("/players/{puuid}/matches/backfill", puuid).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException indisponible) {
            log.warn("Rattrapage non demandé pour {} : {}", puuid, indisponible.getMessage());
            return false;
        }
    }

    @Override
    public boolean requestPreview(String puuid, boolean slow) {
        try {
            restClient.post()
                    .uri(uri -> uri.path("/players/{puuid}/preview").queryParam("slow", slow).build(puuid))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException indisponible) {
            log.warn("Aperçu non demandé pour un joueur recherché ({})", indisponible.getMessage());
            return false;
        }
    }

    @Override
    public List<KnownPlayer> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            List<PlayerSuggestionDto> found = restClient.get()
                    .uri(uri -> uri.path("/players/search")
                            .queryParam("q", query)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PlayerSuggestionDto>>() { });
            return found == null ? List.of() : found.stream().map(contrat::toKnownPlayer).toList();
        } catch (RestClientException indisponible) {
            log.warn("Recherche de compte Riot indisponible ({})", indisponible.getMessage());
            return List.of();
        }
    }
}
