package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.api.dto.RiotIngestProgressDto;
import schultz.thomas.schub.core.team.business.model.PlayerRef;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyStatsService {

    private final PlayerStatsService playerStatsService;
    private final RiotConnectorService riotConnector;

    public MyStatsDto of(User actor, Integer days, Integer champions) {
        PlayerRef joueur = new PlayerRef(actor.getId(), actor.getDisplayName(), actor.getRiotPuuid(),
                actor.getRiotGameName(), actor.getRiotTagLine());
        if (joueur.puuid() == null || joueur.puuid().isBlank()) {
            return vide(joueur, days, StatsState.COMPTE_RIOT_ABSENT, null);
        }
        return of(joueur, days, champions);
    }

    public MyStatsDto of(PlayerRef joueur, Integer days, Integer champions) {
        String puuid = joueur.puuid();
        RiotIngestProgressDto ingest = riotConnector.ingestOf(puuid)
                .map(en -> new RiotIngestProgressDto(en.pending(), en.running(),
                        en.estimatedReadyAt()))
                .orElse(null);
        Instant since = PlayerStatsService.depuis(days);
        Map<String, PlayerStatsService.Figures> figures = playerStatsService.of(
                List.of(puuid), since, PlayerStatsService.bornerChampions(champions));
        PlayerStatsService.Figures chiffres = figures.get(puuid);
        if (chiffres == null) {
            return vide(joueur, days, StatsState.CONNECTEUR_INDISPONIBLE, ingest);
        }
        return new MyStatsDto(
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
                chiffres.state() == StatsState.STATISTIQUES_CONNUES
                        ? playerStatsService.rankings(puuid) : List.of(),
                chiffres.references(),
                Instant.now());
    }

    private static MyStatsDto vide(PlayerRef joueur, Integer days, StatsState state,
                                   RiotIngestProgressDto ingest) {
        return new MyStatsDto(joueur.displayName(), joueur.gameName(),
                joueur.tagLine(), days, state, null, ingest, null, List.of(), List.of(),
                List.of(), List.of(), List.of(), null, Instant.now());
    }
}
