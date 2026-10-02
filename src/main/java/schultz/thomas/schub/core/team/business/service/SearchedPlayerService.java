package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.RiotConnectorBusyException;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.business.service.RiotIdResolution;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.business.service.UnknownRiotAccountException;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.PlayerCollectDto;
import schultz.thomas.schub.core.team.api.dto.RankedStandingDto;
import schultz.thomas.schub.core.team.api.dto.SearchedPlayerDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.model.PlayerRef;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Un joueur recherché par son Riot ID, vu sans compte : ce que Riot rend public, rien de plus.
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchedPlayerService {

    private static final int MAITRISES = 5;

    private final RiotIdResolver resolver;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final RiotConnectorService riotConnector;
    private final PlayerStatsService playerStatsService;
    private final MyStatsService myStats;
    private final MyGamesService myGames;
    private final VisitorBudget budget;

    // light : sans rang ni maîtrises, donc sans appel à Riot. Pour rafraîchir la page pendant une collecte.
    // masteries=false : la page d'un robot, qui ne les affiche pas et ne doit pas entamer le quota.
    public SearchedPlayerDto page(String slug, Integer days, Integer champions, boolean light, boolean masteries) {
        PlayerRef joueur = resolve(slug);
        var rangs = Parallele.lance(() -> light ? List.<RankedStandingDto>of()
                : playerStatsService.rankings(joueur.puuid()));
        var maitrises = Parallele.lance(() -> light || !masteries ? List.<SearchedPlayerDto.MasteryDto>of()
                : maitrises(joueur.puuid()));
        var enCollecte = Parallele.lance(() -> riotConnector.ingestOf(joueur.puuid())
                .map(en -> en.priorityPending() > 0).orElse(false));
        var stats = Parallele.lance(() -> myStats.of(joueur, days, champions));
        Optional<RiotStatsGateway.Coverage> couverture = couverture(joueur.puuid());
        return new SearchedPlayerDto(
                joueur.gameName(),
                joueur.tagLine(),
                slugOf(joueur.gameName(), joueur.tagLine()),
                couverture.map(ligne -> ligne.lastSyncAt() != null).orElse(false),
                couverture.map(RiotStatsGateway.Coverage::analysedMatches).orElse(0L),
                Parallele.attend(enCollecte),
                Parallele.attend(rangs),
                Parallele.attend(maitrises),
                Parallele.attend(stats));
    }

    public MyGamesDto games(String slug, Integer days, Integer limit) {
        return myGames.games(resolve(slug), days, limit);
    }

    public TeamGameDetailDto game(String slug, String matchId, Integer days) {
        return myGames.game(resolve(slug), matchId, days);
    }

    // Jamais de refus : au-delà du budget du visiteur, ses parties arrivent seulement plus lentement.
    public PlayerCollectDto collect(String slug, String visitor) {
        PlayerRef joueur = resolve(slug);
        boolean connu = couverture(joueur.puuid()).map(ligne -> ligne.lastSyncAt() != null).orElse(false);
        if (connu) {
            return new PlayerCollectDto(PlayerCollectDto.Lane.KNOWN);
        }
        boolean rapide = budget.consume(visitor, Instant.now());
        if (!riotConnector.requestPreview(joueur.puuid(), !rapide)) {
            throw new RiotConnectorUnavailableException();
        }
        if (!rapide) {
            log.info("Budget de recherche dépassé : aperçu en voie lente pour {}", joueur.displayName());
        }
        return new PlayerCollectDto(rapide ? PlayerCollectDto.Lane.FAST : PlayerCollectDto.Lane.SLOW);
    }

    public PlayerRef resolve(String slug) {
        int separateur = slug == null ? -1 : slug.lastIndexOf('-');
        if (separateur <= 0 || separateur == slug.length() - 1) {
            throw new IllegalArgumentException("Un Riot ID s'écrit Nom-TAG");
        }
        String gameName = slug.substring(0, separateur).trim();
        String tagLine = slug.substring(separateur + 1).trim();
        RiotIdResolution resolution = resolver.resolveRecent(gameName, tagLine);
        switch (resolution.outcome()) {
            case NOT_FOUND -> throw new UnknownRiotAccountException(gameName + "#" + tagLine);
            case BUSY -> throw new RiotConnectorBusyException();
            case UNAVAILABLE -> throw new RiotConnectorUnavailableException();
            case RESOLVED -> { }
        }
        String nom = resolution.gameName() == null ? gameName : resolution.gameName();
        String tag = resolution.tagLine() == null ? tagLine : resolution.tagLine();
        return new PlayerRef("player:" + slugOf(nom, tag), nom, resolution.puuid(), nom, tag);
    }

    static String slugOf(String gameName, String tagLine) {
        return gameName + "-" + tagLine;
    }

    private Optional<RiotStatsGateway.Coverage> couverture(String puuid) {
        return statsGateway.coverage(List.of(puuid)).orElseGet(List::of).stream()
                .filter(ligne -> puuid.equals(ligne.puuid()))
                .findFirst();
    }

    private List<SearchedPlayerDto.MasteryDto> maitrises(String puuid) {
        Map<Integer, RiotChampionGateway.Champion> catalogue = championGateway.catalogue()
                .map(RiotChampionGateway.Catalogue::parId)
                .orElseGet(Map::of);
        return championGateway.masteries(puuid, MAITRISES).orElseGet(List::of).stream()
                .map(m -> {
                    RiotChampionGateway.Champion champion = catalogue.get(m.championId());
                    return new SearchedPlayerDto.MasteryDto(m.championId(),
                            champion == null ? null : champion.name(),
                            champion == null ? null : champion.iconUrl(),
                            m.level(), m.points());
                })
                .toList();
    }
}
