package schultz.thomas.schub.core.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * L'implémentation HTTP : un appel local sur l'overlay, protégé par le secret interne.
 *
 * <p>Les formes rendues par le connecteur sont redéclarées ici plutôt que partagées — deux
 * services ne partagent pas de classes, c'est ce qui leur permet d'évoluer séparément
 * (migration §5). Seuls les champs lus sont déclarés ; le reste est ignoré.</p>
 */
@Slf4j
@Service
public class HttpRiotConnectorService implements RiotConnectorService {

    private final RestClient restClient;

    public HttpRiotConnectorService(@Qualifier("connectorRiotRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<PlayerIngest> ingestOf(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.empty();
        }
        try {
            IngestResponse response = restClient.get()
                    .uri("/ingest/players/{puuid}", puuid)
                    .retrieve()
                    .body(IngestResponse.class);
            return Optional.ofNullable(response)
                    .map(body -> new PlayerIngest(body.pending(), body.running(), body.estimatedReadyAt()));
        } catch (RestClientException indisponible) {
            log.debug("État d'ingestion indisponible pour {} : {}", puuid, indisponible.getMessage());
            return Optional.empty();
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
    public List<KnownPlayer> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            List<SuggestionResponse> found = restClient.get()
                    .uri(uri -> uri.path("/players/search")
                            .queryParam("q", query)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SuggestionResponse>>() { });
            return found == null ? List.of() : found.stream().map(SuggestionResponse::toKnownPlayer).toList();
        } catch (RestClientException indisponible) {
            log.warn("Recherche de compte Riot indisponible ({})", indisponible.getMessage());
            return List.of();
        }
    }

    record IngestResponse(long pending, long running, Instant estimatedReadyAt) {
    }

    record SuggestionResponse(String puuid, String gameName, String tagLine, String riotId,
                              long matchCount, List<PositionResponse> positions, Instant lastPlayedAt,
                              Instant observedAt, String source) {

        KnownPlayer toKnownPlayer() {
            List<PositionPlayed> postes = positions == null ? List.of()
                    : positions.stream().map(PositionResponse::toPositionPlayed).toList();
            return new KnownPlayer(puuid, gameName, tagLine, riotId, matchCount, postes, lastPlayedAt,
                    observedAt, source);
        }
    }

    record PositionResponse(String position, long matches) {

        PositionPlayed toPositionPlayed() {
            return new PositionPlayed(position, matches);
        }
    }
}
