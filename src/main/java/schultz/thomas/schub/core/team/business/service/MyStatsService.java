package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.api.dto.RiotIngestProgressDto;
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
        String puuid = actor.getRiotPuuid();
        if (puuid == null || puuid.isBlank()) {
            return vide(actor, days, StatsState.COMPTE_RIOT_ABSENT, null);
        }
        RiotIngestProgressDto ingest = riotConnector.ingestOf(puuid)
                .map(en -> new RiotIngestProgressDto(en.pending(), en.running(),
                        en.estimatedReadyAt()))
                .orElse(null);
        Instant since = PlayerStatsService.depuis(days);
        Map<String, PlayerStatsService.Figures> figures = playerStatsService.of(
                List.of(puuid), since, PlayerStatsService.bornerChampions(champions));
        PlayerStatsService.Figures chiffres = figures.get(puuid);
        if (chiffres == null) {
            return vide(actor, days, StatsState.CONNECTEUR_INDISPONIBLE, ingest);
        }
        return new MyStatsDto(
                actor.getDisplayName(),
                actor.getRiotGameName(),
                actor.getRiotTagLine(),
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
                chiffres.radar(),
                playerStatsService.scale(),
                Instant.now());
    }

    private static MyStatsDto vide(User actor, Integer days, StatsState state,
                                   RiotIngestProgressDto ingest) {
        return new MyStatsDto(actor.getDisplayName(), actor.getRiotGameName(),
                actor.getRiotTagLine(), days, state, null, ingest, null, List.of(), List.of(),
                List.of(), List.of(), List.of(), null, null, Instant.now());
    }
}
