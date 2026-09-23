package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final ParameterizedTypeReference<List<InsightResponse>> INSIGHTS =
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
    public Optional<List<Bucket>> aggregate(List<String> puuids, Grouping groupBy, Scope scope,
                                            Instant since) {
        if (puuids == null || puuids.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            List<BucketResponse> reponse = restClient.post()
                    .uri("/stats/aggregate")
                    .body(new AggregateRequest(puuids, groupBy.name(), scope.name(), since))
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
    public Optional<Scale> scale() {
        try {
            ScaleResponse reponse = restClient.get().uri("/stats/scale").retrieve().body(ScaleResponse.class);
            if (reponse == null) {
                return Optional.empty();
            }
            Map<String, Bound> bornes = new LinkedHashMap<>();
            if (reponse.bounds() != null) {
                reponse.bounds().forEach((cle, borne) -> bornes.put(cle, new Bound(borne.low(), borne.high())));
            }
            return Optional.of(new Scale(reponse.computedAt(), reponse.population(), reponse.minimumGames(),
                    reponse.recentPatches() == null ? List.of() : reponse.recentPatches(), bornes));
        } catch (RestClientException e) {
            log.warn("Bornes des indicateurs non obtenues ({})", e.getMessage());
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
                    .body(new SharedMatchesRequest(puuids, minimumPlayers, since, limit, true))
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

    @Override
    public Optional<List<Insight>> insights(List<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            List<InsightResponse> reponse = restClient.post()
                    .uri("/stats/match-insights")
                    .body(new MatchIdsRequest(matchIds))
                    .retrieve()
                    .body(INSIGHTS);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream()
                    .map(row -> new Insight(row.matchId(), row.timelineAvailable(), row.ranksObservedAt(),
                            row.participants() == null ? List.of() : row.participants().stream()
                                    .map(p -> new InsightPlayer(p.puuid(), p.side(), p.position(),
                                            p.championId(), toStanding(p.solo()), toStanding(p.flex()), p.at15()))
                                    .toList()))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Rangs et chiffres à 15 minutes non obtenus ({})", e.getMessage());
            return Optional.empty();
        }
    }

    private static Standing toStanding(StandingResponse row) {
        return row == null ? null : new Standing(row.queue(), row.riotQueueType(), row.tier(),
                row.division(), row.leaguePoints(), row.wins(), row.losses(), row.hotStreak(),
                row.inactive(), row.observedAt());
    }

    private static Bucket toBucket(BucketResponse row) {
        return new Bucket(row.puuid(), row.key(), row.championName(), row.games(), row.wins(),
                row.kills(), row.deaths(), row.assists(), row.minionsKilled(), row.goldEarned(),
                row.damageToChampions(), row.damageTaken(), row.visionScore(), row.afkGames(),
                row.secondsPlayed(),
                row.firstPlayedAt(), row.lastPlayedAt());
    }

    private static SharedMatch toSharedMatch(SharedMatchResponse row) {
        List<SharedMatchPlayerResponse> tous = row.players() == null ? List.of() : row.players();
        return new SharedMatch(row.matchId(), row.startedAt(), row.durationSeconds(), row.queueId(),
                row.queue(), row.patch(), row.presentPlayers(), row.splitSides(), row.win(),
                tous.stream().filter(SharedMatchPlayerResponse::requested)
                        .map(ConnectorRiotStatsGateway::toPlayer).toList(),
                tous.stream().filter(player -> !player.requested())
                        .map(ConnectorRiotStatsGateway::toPlayer).toList());
    }

    private static SharedMatchPlayer toPlayer(SharedMatchPlayerResponse player) {
        return new SharedMatchPlayer(player.puuid(), player.championId(), player.championName(),
                player.position(), player.win(), player.side(), player.kills(), player.deaths(),
                player.assists(), player.minionsKilled(), player.goldEarned(),
                player.damageToChampions(), player.damageTaken(), player.visionScore(), player.afk());
    }

    record AggregateRequest(List<String> puuids, String groupBy, String scope, Instant since) {
    }

    record PuuidsRequest(List<String> puuids) {
    }

    // enrich : toute partie partagée d'ici est une partie d'équipe, dont on veut timeline et rangs.
    record SharedMatchesRequest(List<String> puuids, int minimumPlayers, Instant since,
                                Integer limit, boolean enrich) {
    }

    record MatchIdsRequest(List<String> matchIds) {
    }

    record InsightResponse(String matchId, boolean timelineAvailable, Instant ranksObservedAt,
                           List<InsightPlayerResponse> participants) {
    }

    record InsightPlayerResponse(String puuid, int side, String position, int championId,
                                 StandingResponse solo, StandingResponse flex, At15 at15) {
    }

    record BucketResponse(String puuid, String key, String championName, long games, long wins,
                          long kills, long deaths, long assists, long minionsKilled,
                          long goldEarned, long damageToChampions, long damageTaken, long visionScore,
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
                                     int damageToChampions, int damageTaken, int visionScore,
                                     boolean afk, boolean requested) {
    }

    record ScaleResponse(Instant computedAt, int population, int minimumGames, List<String> recentPatches,
                         Map<String, BoundResponse> bounds) {
    }

    record BoundResponse(double low, double high) {
    }

    record StandingResponse(String queue, String riotQueueType, String tier, String division,
                            int leaguePoints, int wins, int losses, boolean hotStreak,
                            boolean inactive, Instant observedAt) {
    }
}
