package schultz.thomas.schub.core.lol.stats.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.stats.business.model.MyStats;
import schultz.thomas.schub.core.lol.stats.business.model.PlayerRef;
import schultz.thomas.schub.core.lol.stats.business.model.RiotIngestProgress;
import schultz.thomas.schub.core.lol.stats.business.model.StatsState;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyStatsService {

    private final PlayerStatsService playerStatsService;
    private final RiotConnectorService riotConnector;

    public MyStats of(User actor, Integer days, Integer champions) {
        PlayerRef joueur = new PlayerRef(actor.getId(), actor.getDisplayName(), actor.getRiotPuuid(),
                actor.getRiotGameName(), actor.getRiotTagLine());
        if (joueur.puuid() == null || joueur.puuid().isBlank()) {
            return vide(joueur, days, StatsState.COMPTE_RIOT_ABSENT, null);
        }
        return of(joueur, days, champions);
    }

    public MyStats of(PlayerRef joueur, Integer days, Integer champions) {
        String puuid = joueur.puuid();
        RiotIngestProgress ingest = riotConnector.ingestOf(puuid)
                .map(en -> new RiotIngestProgress(en.pending(), en.running(),
                        en.estimatedReadyAt()))
                .orElse(null);
        Instant since = PlayerStatsService.depuis(days);
        Map<String, PlayerStatsService.Figures> figures = playerStatsService.of(
                List.of(puuid), since, PlayerStatsService.bornerChampions(champions));
        PlayerStatsService.Figures chiffres = figures.get(puuid);
        if (chiffres == null) {
            return vide(joueur, days, StatsState.CONNECTEUR_INDISPONIBLE, ingest);
        }
        return new MyStats(
                joueur.displayName(),
                joueur.gameName(),
                joueur.tagLine(),
                days,
                chiffres.state(),
                chiffres.coverage(),
                ingest,
                chiffres.overall(),
                chiffres.champions(),
                chiffres.positions(),
                chiffres.queues(),
                chiffres.months(),
                chiffres.patches(),
                chiffres.state() == StatsState.STATISTIQUES_CONNUES
                        ? playerStatsService.rankings(puuid) : List.of(),
                chiffres.references(),
                Instant.now());
    }

    private static MyStats vide(PlayerRef joueur, Integer days, StatsState state,
                                   RiotIngestProgress ingest) {
        return new MyStats(joueur.displayName(), joueur.gameName(),
                joueur.tagLine(), days, state, null, ingest, null, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null, Instant.now());
    }
}
