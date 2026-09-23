package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.team.api.dto.MetricScaleDto;
import schultz.thomas.schub.core.team.api.dto.RadarDto;
import schultz.thomas.schub.core.team.api.dto.RankedStandingDto;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;
import schultz.thomas.schub.core.team.api.dto.StatsCoverageDto;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    public static final int CHAMPIONS_DEFAUT = 8;
    public static final int CHAMPIONS_MAX = 30;
    private static final int MOIS_RENDUS = 12;
    private static final int PATCHS_PAR_SERIE = 2;

    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final RiotConnectorService riotConnector;

    public record Figures(
            StatsState state,
            StatsCoverageDto coverage,
            StatLineDto overall,
            List<StatLineDto> champions,
            List<StatLineDto> positions,
            List<StatLineDto> queues,
            List<StatLineDto> months,
            RadarDto radar,
            RiotStatsGateway.Bucket total
    ) {
    }

    public Map<String, Figures> of(List<String> puuids, Instant since, int championsMax) {
        List<String> propres = propres(puuids);
        if (propres.isEmpty()) {
            return Map.of();
        }
        Optional<List<RiotStatsGateway.Bucket>> totaux = statsGateway.aggregate(propres,
                RiotStatsGateway.Grouping.OVERALL, RiotStatsGateway.Scope.RIFT, since);
        if (totaux.isEmpty()) {
            return indisponible(propres);
        }
        Map<String, RiotStatsGateway.Bucket> parPuuid = indexe(totaux.get());
        Map<String, RiotStatsGateway.Coverage> couverture = statsGateway.coverage(propres)
                .map(rows -> {
                    Map<String, RiotStatsGateway.Coverage> index = new LinkedHashMap<>();
                    rows.forEach(row -> index.put(row.puuid(), row));
                    return index;
                })
                .orElseGet(Map::of);

        Optional<RiotChampionGateway.Catalogue> catalogue = championGateway.catalogue();
        Map<String, List<RiotStatsGateway.Bucket>> champions =
                groupe(propres, RiotStatsGateway.Grouping.CHAMPION, RiotStatsGateway.Scope.RIFT, since);
        Map<String, List<RiotStatsGateway.Bucket>> positions =
                groupe(propres, RiotStatsGateway.Grouping.POSITION, RiotStatsGateway.Scope.RIFT, since);
        Map<String, List<RiotStatsGateway.Bucket>> queues =
                groupe(propres, RiotStatsGateway.Grouping.QUEUE, RiotStatsGateway.Scope.ALL, since);
        Map<String, List<RiotStatsGateway.Bucket>> mois =
                groupe(propres, RiotStatsGateway.Grouping.MONTH, RiotStatsGateway.Scope.RIFT, since);
        List<String> patchs = statsGateway.scale().map(RiotStatsGateway.Scale::recentPatches).orElseGet(List::of);
        Map<String, List<RiotStatsGateway.Bucket>> parPatch = patchs.isEmpty() ? Map.of()
                : groupe(propres, RiotStatsGateway.Grouping.PATCH, RiotStatsGateway.Scope.RIFT, null);

        Map<String, Figures> figures = new LinkedHashMap<>();
        for (String puuid : propres) {
            RiotStatsGateway.Bucket total = parPuuid.getOrDefault(puuid, StatLines.vide(puuid));
            RiotStatsGateway.Coverage couvre = couverture.get(puuid);
            figures.put(puuid, new Figures(
                    etat(total, couvre, puuid),
                    toCoverage(couvre, total),
                    StatLines.of(total, null, null, null),
                    lignesChampions(champions.getOrDefault(puuid, List.of()), total, catalogue,
                            championsMax),
                    lignes(positions.getOrDefault(puuid, List.of()), total),
                    lignes(queues.getOrDefault(puuid, List.of()), null),
                    lignesMois(mois.getOrDefault(puuid, List.of())),
                    radar(puuid, parPatch.getOrDefault(puuid, List.of()), patchs),
                    total));
        }
        return figures;
    }

    public MetricScaleDto scale() {
        return statsGateway.scale()
                .filter(scale -> !scale.bounds().isEmpty())
                .map(scale -> {
                    Map<String, MetricScaleDto.Bound> bornes = new LinkedHashMap<>();
                    scale.bounds().forEach((cle, borne) ->
                            bornes.put(cle, new MetricScaleDto.Bound(borne.low(), borne.high())));
                    return new MetricScaleDto(scale.computedAt(), scale.population(), scale.minimumGames(),
                            bornes);
                })
                .orElse(null);
    }

    // Les patchs du jeu, pas ceux du joueur : un patch où il n'a pas joué reste dans sa série, à vide.
    static RadarDto radar(String puuid, List<RiotStatsGateway.Bucket> parPatch, List<String> patchs) {
        if (patchs.isEmpty()) {
            return null;
        }
        List<String> recents = patchs.subList(0, Math.min(PATCHS_PAR_SERIE, patchs.size()));
        List<String> precedents = patchs.subList(recents.size(),
                Math.min(2 * PATCHS_PAR_SERIE, patchs.size()));
        return new RadarDto(recents, precedents, serie(puuid, parPatch, recents),
                serie(puuid, parPatch, precedents));
    }

    private static StatLineDto serie(String puuid, List<RiotStatsGateway.Bucket> parPatch, List<String> patchs) {
        List<RiotStatsGateway.Bucket> retenus = parPatch.stream()
                .filter(bucket -> patchs.contains(bucket.key()))
                .toList();
        if (retenus.isEmpty()) {
            return null;
        }
        return StatLines.of(StatLines.additionne(puuid, String.join(",", patchs), retenus), null, null, null);
    }

    public List<RankedStandingDto> rankings(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return List.of();
        }
        return statsGateway.rankings(puuid)
                .orElseGet(List::of).stream()
                .map(standing -> new RankedStandingDto(standing.queue(), standing.riotQueueType(),
                        standing.tier(), standing.division(), standing.leaguePoints(),
                        standing.wins(), standing.losses(), standing.hotStreak(),
                        standing.inactive(), standing.observedAt()))
                .toList();
    }

    public static int bornerChampions(Integer demande) {
        if (demande == null) {
            return CHAMPIONS_DEFAUT;
        }
        return (int) Math.clamp(demande.longValue(), 1, CHAMPIONS_MAX);
    }

    public static Instant depuis(Integer days) {
        if (days == null || days <= 0) {
            return null;
        }
        return Instant.now().minusSeconds(days * 86_400L);
    }

    private Map<String, Figures> indisponible(List<String> puuids) {
        Map<String, Figures> figures = new LinkedHashMap<>();
        for (String puuid : puuids) {
            RiotStatsGateway.Bucket vide = StatLines.vide(puuid);
            figures.put(puuid, new Figures(StatsState.CONNECTEUR_INDISPONIBLE, null,
                    StatLines.of(vide, null, null, null), List.of(), List.of(), List.of(),
                    List.of(), null, vide));
        }
        return figures;
    }

    private StatsState etat(RiotStatsGateway.Bucket total, RiotStatsGateway.Coverage couverture,
                            String puuid) {
        if (total.games() > 0 || (couverture != null && couverture.analysedMatches() > 0)) {
            return StatsState.STATISTIQUES_CONNUES;
        }
        Optional<RiotConnectorService.PlayerIngest> ingest = riotConnector.ingestOf(puuid);
        if (ingest.filter(en -> en.pending() + en.running() > 0).isPresent()) {
            return StatsState.INGESTION_EN_COURS;
        }
        if (couverture != null && couverture.knownMatches() > 0) {
            return StatsState.INGESTION_EN_COURS;
        }
        return StatsState.AUCUNE_PARTIE;
    }

    private static StatsCoverageDto toCoverage(RiotStatsGateway.Coverage couverture,
                                               RiotStatsGateway.Bucket total) {
        if (couverture == null) {
            return new StatsCoverageDto(total.games(), total.games(), 0, false,
                    total.firstPlayedAt(), total.lastPlayedAt(), null);
        }
        return new StatsCoverageDto(
                couverture.analysedMatches(),
                couverture.knownMatches(),
                Math.max(0, couverture.knownMatches() - couverture.analysedMatches()),
                couverture.tracked(),
                couverture.firstPlayedAt(),
                couverture.lastPlayedAt(),
                couverture.lastSyncAt());
    }

    private Map<String, List<RiotStatsGateway.Bucket>> groupe(
            List<String> puuids, RiotStatsGateway.Grouping groupBy, RiotStatsGateway.Scope scope,
            Instant since) {
        Map<String, List<RiotStatsGateway.Bucket>> parPuuid = new LinkedHashMap<>();
        statsGateway.aggregate(puuids, groupBy, scope, since).orElseGet(List::of)
                .forEach(bucket -> parPuuid
                        .computeIfAbsent(bucket.puuid(), key -> new java.util.ArrayList<>())
                        .add(bucket));
        return parPuuid;
    }

    private static Map<String, RiotStatsGateway.Bucket> indexe(List<RiotStatsGateway.Bucket> buckets) {
        Map<String, RiotStatsGateway.Bucket> parPuuid = new LinkedHashMap<>();
        buckets.forEach(bucket -> parPuuid.put(bucket.puuid(), bucket));
        return parPuuid;
    }

    private static List<StatLineDto> lignes(List<RiotStatsGateway.Bucket> buckets,
                                            RiotStatsGateway.Bucket total) {
        return buckets.stream()
                .sorted(Comparator.comparingLong(RiotStatsGateway.Bucket::games).reversed())
                .map(bucket -> StatLines.of(bucket, null, null,
                        total == null ? null : StatLines.versusRest(bucket, total)))
                .toList();
    }

    private static List<StatLineDto> lignesChampions(
            List<RiotStatsGateway.Bucket> buckets, RiotStatsGateway.Bucket total,
            Optional<RiotChampionGateway.Catalogue> catalogue, int max) {
        return buckets.stream()
                .sorted(Comparator.comparingLong(RiotStatsGateway.Bucket::games).reversed())
                .limit(max)
                .map(bucket -> {
                    RiotChampionGateway.Champion champion = catalogue
                            .map(cat -> cat.parId().get(entier(bucket.key())))
                            .orElse(null);
                    String nom = champion != null ? champion.name() : bucket.championName();
                    return StatLines.of(bucket, nom, champion == null ? null : champion.iconUrl(),
                            StatLines.versusRest(bucket, total));
                })
                .toList();
    }

    private static List<StatLineDto> lignesMois(List<RiotStatsGateway.Bucket> buckets) {
        List<StatLineDto> lignes = buckets.stream()
                .sorted(Comparator.comparing(RiotStatsGateway.Bucket::key))
                .map(bucket -> StatLines.of(bucket, null, null, null))
                .toList();
        return lignes.size() <= MOIS_RENDUS ? lignes
                : lignes.subList(lignes.size() - MOIS_RENDUS, lignes.size());
    }

    private static int entier(String valeur) {
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static List<String> propres(List<String> puuids) {
        if (puuids == null) {
            return List.of();
        }
        return puuids.stream()
                .filter(puuid -> puuid != null && !puuid.isBlank())
                .distinct()
                .toList();
    }
}
