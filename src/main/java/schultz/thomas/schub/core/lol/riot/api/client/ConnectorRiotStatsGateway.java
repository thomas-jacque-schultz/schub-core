package schultz.thomas.schub.core.lol.riot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.api.client.riot.ChampionReferenceGridDto;
import schultz.thomas.schub.core.api.client.riot.MatchIdsRequest;
import schultz.thomas.schub.core.api.client.riot.MatchInsightsDto;
import schultz.thomas.schub.core.api.client.riot.MatchPlayerMetricsDto;
import schultz.thomas.schub.core.api.client.riot.ParticipationBucketDto;
import schultz.thomas.schub.core.api.client.riot.PatchStartDto;
import schultz.thomas.schub.core.api.client.riot.Player;
import schultz.thomas.schub.core.api.client.riot.PlayerCoverageDto;
import schultz.thomas.schub.core.api.client.riot.PlayerReferencesDto;
import schultz.thomas.schub.core.api.client.riot.PuuidListRequest;
import schultz.thomas.schub.core.api.client.riot.RankedStandingDto;
import schultz.thomas.schub.core.api.client.riot.ReferenceGridDto;
import schultz.thomas.schub.core.api.client.riot.ReferencesQueryDto;
import schultz.thomas.schub.core.api.client.riot.SharedMatchesDto;
import schultz.thomas.schub.core.api.client.riot.SharedMatchesQuery;
import schultz.thomas.schub.core.api.client.riot.StatsQuery;
import schultz.thomas.schub.core.api.client.riot.TimelineHabitsDto;
import schultz.thomas.schub.core.lol.riot.business.model.ChampionGrid;
import schultz.thomas.schub.core.lol.riot.business.model.ReferenceGrid;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ConnectorRiotStatsGateway implements RiotStatsGateway {

    private static final ParameterizedTypeReference<List<ParticipationBucketDto>> BUCKETS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<PlayerCoverageDto>> COUVERTURE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<MatchInsightsDto>> INSIGHTS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<PlayerReferencesDto>> REFERENTIELS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<RankedStandingDto>> CLASSEMENTS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<PatchStartDto>> PATCHS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<MatchPlayerMetricsDto>> METRIQUES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RiotContractMapper contrat;

    public ConnectorRiotStatsGateway(@Qualifier("connectorRiotRestClient") RestClient restClient,
                                     RiotContractMapper contrat) {
        this.restClient = restClient;
        this.contrat = contrat;
    }

    @Override
    public Optional<List<Bucket>> aggregate(List<String> puuids, Grouping groupBy, Scope scope,
                                            Instant since) {
        return aggregate(puuids, groupBy, scope, since, null);
    }

    @Override
    public Optional<List<Bucket>> aggregate(List<String> puuids, Grouping groupBy, Scope scope,
                                            Instant since, List<String> matchIds) {
        if (puuids == null || puuids.isEmpty() || (matchIds != null && matchIds.isEmpty())) {
            return Optional.of(List.of());
        }
        try {
            List<ParticipationBucketDto> reponse = restClient.post()
                    .uri("/stats/aggregate")
                    .body(new StatsQuery().puuids(puuids).groupBy(StatsQuery.GroupByEnum.fromValue(groupBy.name()))
                            .scope(StatsQuery.ScopeEnum.fromValue(scope.name())).since(since).matchIds(matchIds))
                    .retrieve()
                    .body(BUCKETS);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream().map(contrat::toBucket).toList());
        } catch (RestClientException e) {
            log.warn("Agrégats {} non obtenus ({}) — l'écran dira pourquoi il est vide",
                    groupBy, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<References>> references(List<ReferenceRequest> joueurs) {
        if (joueurs == null || joueurs.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            return Optional.ofNullable(restClient.post()
                    .uri("/stats/references")
                    .body(new ReferencesQueryDto().players(joueurs.stream()
                            .map(j -> new Player().puuid(j.puuid()).position(Player.PositionEnum.fromValue(j.position()))
                                    .since(j.since()))
                            .toList()))
                    .retrieve()
                    .body(REFERENTIELS))
                    .map(lus -> lus.stream().map(contrat::toReferences).toList());
        } catch (RestClientException e) {
            log.warn("Référentiels du radar non obtenus ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<PatchStart>> patches(int count) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uri -> uri.path("/stats/patches").queryParam("count", count).build())
                    .retrieve()
                    .body(PATCHS))
                    .map(lus -> lus.stream().map(contrat::toPatchStart).toList());
        } catch (RestClientException e) {
            log.warn("Calendrier des patchs non obtenu ({})", e.getMessage());
            return Optional.empty();
        }
    }

    // 404 = pas encore de référence pour ce poste : « on ne sait pas », comme une panne.
    @Override
    public Optional<ReferenceGrid> referenceGrid(String position, String scope, String tier, String patch) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uri -> {
                        uri.path("/stats/references/{position}").queryParam("scope", scope);
                        if (tier != null) {
                            uri.queryParam("tier", tier);
                        }
                        if (patch != null) {
                            uri.queryParam("patch", patch);
                        }
                        return uri.build(position);
                    })
                    .retrieve()
                    .body(ReferenceGridDto.class))
                    .map(contrat::toGrid);
        } catch (RestClientException e) {
            log.warn("Référentiel {} {} non obtenu ({})", position, scope, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<ChampionGrid> championGrid(int championId, String tier) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uri -> uri.path("/stats/references/champions/{championId}").queryParam("tier", tier)
                            .build(championId))
                    .retrieve()
                    .body(ChampionReferenceGridDto.class))
                    .map(contrat::toGrid);
        } catch (RestClientException e) {
            log.debug("Référentiel du champion {} non obtenu ({})", championId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<ReferenceGrid> teamGrid() {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/stats/references/team")
                    .retrieve()
                    .body(ReferenceGridDto.class))
                    .map(contrat::toGrid);
        } catch (RestClientException e) {
            log.warn("Référentiel d'équipe non obtenu ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Coverage>> coverage(List<String> puuids) {
        if (puuids == null || puuids.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            List<PlayerCoverageDto> reponse = restClient.post()
                    .uri("/stats/coverage")
                    .body(new PuuidListRequest().puuids(puuids))
                    .retrieve()
                    .body(COUVERTURE);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream().map(contrat::toCoverage).toList());
        } catch (RestClientException e) {
            log.warn("Couverture non obtenue ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<SharedMatches> sharedMatches(List<String> puuids, int minimumPlayers,
                                                 Instant since, Integer limit) {
        return communes(puuids, minimumPlayers, demande(puuids, minimumPlayers, since, limit, true, null));
    }

    @Override
    public Optional<SharedMatches> playerMatches(String puuid, Instant since, Integer limit) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.of(new SharedMatches(1, 0, false, List.of()));
        }
        return communes(List.of(puuid), 1, demande(List.of(puuid), 1, since, limit, false, null));
    }

    @Override
    public Optional<SharedMatches> sharedMatchesAmong(List<String> puuids, int minimumPlayers, List<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Optional.of(new SharedMatches(minimumPlayers, 0, false, List.of()));
        }
        return communes(puuids, minimumPlayers, demande(puuids, minimumPlayers, null, matchIds.size(), true, matchIds));
    }

    @Override
    public Optional<List<PlayerMetrics>> matchMetrics(String matchId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/stats/matches/{matchId}/metrics", matchId)
                    .retrieve()
                    .body(METRIQUES))
                    .map(lus -> lus.stream().map(contrat::toMetrics).toList());
        } catch (RestClientException e) {
            log.warn("Indicateurs de la partie {} non obtenus ({})", matchId, e.getMessage());
            return Optional.empty();
        }
    }

    // enrich : toute partie partagée d'ici est une partie d'équipe, dont on veut timeline et rangs.
    private static SharedMatchesQuery demande(List<String> puuids, int minimumPlayers, Instant since, Integer limit,
                                              boolean enrich, List<String> matchIds) {
        return new SharedMatchesQuery().puuids(puuids).minimumPlayers(minimumPlayers).since(since).limit(limit)
                .enrich(enrich).matchIds(matchIds);
    }

    private Optional<SharedMatches> communes(List<String> puuids, int minimumPlayers, SharedMatchesQuery demande) {
        if (puuids == null || puuids.size() < minimumPlayers) {
            return Optional.of(new SharedMatches(minimumPlayers, 0, false, List.of()));
        }
        try {
            SharedMatchesDto reponse = restClient.post()
                    .uri("/stats/shared-matches")
                    .body(demande)
                    .retrieve()
                    .body(SharedMatchesDto.class);
            if (reponse == null || reponse.getMatches() == null) {
                return Optional.empty();
            }
            return Optional.of(new SharedMatches(valeur(reponse.getMinimumPlayers()), valeur(reponse.getTotalMatches()),
                    Boolean.TRUE.equals(reponse.getTruncated()),
                    reponse.getMatches().stream().map(contrat::toSharedMatch).toList()));
        } catch (RestClientException e) {
            log.warn("Parties communes non obtenues ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<TimelineHabits> timelineHabits(String puuid, Instant since) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uri -> uri.path("/stats/players/{puuid}/timeline-habits")
                            .queryParamIfPresent("since", Optional.ofNullable(since)).build(puuid))
                    .retrieve().body(TimelineHabitsDto.class))
                    .map(contrat::toHabits);
        } catch (RestClientException e) {
            log.warn("Habitudes de timeline non obtenues ({})", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Standing>> rankings(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.of(List.of());
        }
        try {
            List<RankedStandingDto> reponse = restClient.get()
                    .uri("/players/{puuid}/rankings", puuid)
                    .retrieve()
                    .body(CLASSEMENTS);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream().map(contrat::toStanding).toList());
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
            List<MatchInsightsDto> reponse = restClient.post()
                    .uri("/stats/match-insights")
                    .body(new MatchIdsRequest().matchIds(matchIds))
                    .retrieve()
                    .body(INSIGHTS);
            return reponse == null ? Optional.empty() : Optional.of(reponse.stream().map(contrat::toInsight).toList());
        } catch (RestClientException e) {
            log.warn("Rangs et chiffres à 15 minutes non obtenus ({})", e.getMessage());
            return Optional.empty();
        }
    }

    private static int valeur(Integer nombre) {
        return nombre == null ? 0 : nombre;
    }

    private static long valeur(Long nombre) {
        return nombre == null ? 0 : nombre;
    }
}
