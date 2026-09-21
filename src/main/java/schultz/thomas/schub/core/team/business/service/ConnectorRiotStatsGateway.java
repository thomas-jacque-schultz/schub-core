package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ConnectorRiotStatsGateway implements RiotStatsGateway {

    private static final ParameterizedTypeReference<List<BucketResponse>> BUCKETS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<CoverageResponse>> COUVERTURE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<StandingResponse>> CLASSEMENTS =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public ConnectorRiotStatsGateway(@Qualifier("connectorRiotRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<List<Bucket>> aggregate(List<String> puuids, Grouping groupBy, Instant since) {
        if (puuids == null || puuids.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            List<BucketResponse> reponse = restClient.post()
                    .uri("/stats/aggregate")
                    .body(new AggregateRequest(puuids, groupBy.name(), since))
                    .retrieve()
                    .body(BUCKETS);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream()
                    .map(ConnectorRiotStatsGateway::toBucket)
                    .toList());
        } catch (RestClientException e) {
            log.warn("Agrégats {} non obtenus ({}) — l'écran dira pourquoi il est vide",
                    groupBy, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Coverage>> coverage(List<String> puuids) {
        if (puuids == null || puuids.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            List<CoverageResponse> reponse = restClient.post()
                    .uri("/stats/coverage")
                    .body(new PuuidsRequest(puuids))
                    .retrieve()
                    .body(COUVERTURE);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream()
                    .map(row -> new Coverage(row.puuid(), row.tracked(), row.knownMatches(),
                            row.analysedMatches(), row.firstPlayedAt(), row.lastPlayedAt(),
                            row.lastSyncAt()))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Couverture non obtenue ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<SharedMatches> sharedMatches(List<String> puuids, int minimumPlayers,
                                                 Instant since, Integer limit) {
        if (puuids == null || puuids.size() < minimumPlayers) {
            return Optional.of(new SharedMatches(minimumPlayers, 0, false, List.of()));
        }
        try {
            SharedMatchesResponse reponse = restClient.post()
                    .uri("/stats/shared-matches")
                    .body(new SharedMatchesRequest(puuids, minimumPlayers, since, limit))
                    .retrieve()
                    .body(SharedMatchesResponse.class);
            if (reponse == null || reponse.matches() == null) {
                return Optional.empty();
            }
            return Optional.of(new SharedMatches(reponse.minimumPlayers(), reponse.totalMatches(),
                    reponse.truncated(), reponse.matches().stream()
                    .map(ConnectorRiotStatsGateway::toSharedMatch)
                    .toList()));
        } catch (RestClientException e) {
            log.warn("Parties communes non obtenues ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Standing>> rankings(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.of(List.of());
        }
        try {
            List<StandingResponse> reponse = restClient.get()
                    .uri("/players/{puuid}/rankings", puuid)
                    .retrieve()
                    .body(CLASSEMENTS);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream()
                    .map(row -> new Standing(row.queue(), row.riotQueueType(), row.tier(),
                            row.division(), row.leaguePoints(), row.wins(), row.losses(),
                            row.hotStreak(), row.inactive(), row.observedAt()))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Classement non obtenu pour un joueur ({})", e.getMessage());
            return Optional.empty();
        }
    }

    private static Bucket toBucket(BucketResponse row) {
        return new Bucket(row.puuid(), row.key(), row.championName(), row.games(), row.wins(),
                row.kills(), row.deaths(), row.assists(), row.minionsKilled(), row.goldEarned(),
                row.damageToChampions(), row.visionScore(), row.afkGames(), row.secondsPlayed(),
                row.firstPlayedAt(), row.lastPlayedAt());
    }

    private static SharedMatch toSharedMatch(SharedMatchResponse row) {
        return new SharedMatch(row.matchId(), row.startedAt(), row.durationSeconds(), row.queueId(),
                row.queue(), row.patch(), row.presentPlayers(), row.splitSides(), row.win(),
                row.players() == null ? List.of() : row.players().stream()
                        .map(player -> new SharedMatchPlayer(player.puuid(), player.championId(),
                                player.championName(), player.position(), player.win(),
                                player.side(), player.kills(), player.deaths(), player.assists(),
                                player.minionsKilled(), player.goldEarned(),
                                player.damageToChampions(), player.visionScore(), player.afk()))
                        .toList());
    }

    // --- les formes du connecteur, redéclarées : deux services ne partagent pas de classes ---

    record AggregateRequest(List<String> puuids, String groupBy, Instant since) {
    }

    record PuuidsRequest(List<String> puuids) {
    }

    record SharedMatchesRequest(List<String> puuids, int minimumPlayers, Instant since,
                                Integer limit) {
    }

    record BucketResponse(String puuid, String key, String championName, long games, long wins,
                          long kills, long deaths, long assists, long minionsKilled,
                          long goldEarned, long damageToChampions, long visionScore,
                          long afkGames, long secondsPlayed, Instant firstPlayedAt,
                          Instant lastPlayedAt) {
    }

    record CoverageResponse(String puuid, boolean tracked, long knownMatches, long analysedMatches,
                            Instant firstPlayedAt, Instant lastPlayedAt, Instant firstSyncAt,
                            Instant lastSyncAt) {
    }

    record SharedMatchesResponse(int minimumPlayers, int pool, long totalMatches, boolean truncated,
                                 List<SharedMatchResponse> matches) {
    }

    record SharedMatchResponse(String matchId, Instant startedAt, long durationSeconds, int queueId,
                               String queue, String patch, boolean complete, int presentPlayers,
                               boolean splitSides, Boolean win,
                               List<SharedMatchPlayerResponse> players) {
    }

    record SharedMatchPlayerResponse(String puuid, int championId, String championName,
                                     String position, boolean win, int side, int kills, int deaths,
                                     int assists, int minionsKilled, int goldEarned,
                                     int damageToChampions, int visionScore, boolean afk) {
    }

    record StandingResponse(String queue, String riotQueueType, String tier, String division,
                            int leaguePoints, int wins, int losses, boolean hotStreak,
                            boolean inactive, Instant observedAt) {
    }
}
