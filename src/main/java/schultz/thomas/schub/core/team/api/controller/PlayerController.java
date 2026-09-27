package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.augur.api.dto.FindingDto;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.business.service.AugurService;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.PlayerCollectDto;
import schultz.thomas.schub.core.team.api.dto.SearchedPlayerDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.service.SearchedPlayerService;
import schultz.thomas.schub.core.team.business.service.StatsWindows;

// Public : un joueur se désigne par son Riot ID (Nom-TAG), jamais par un puuid.
@RestController
@RequestMapping("/players/{riotId}")
@RequiredArgsConstructor
public class PlayerController {

    public static final String VISITOR_ID = "X-Visitor-Id";

    private final SearchedPlayerService players;
    private final StatsWindows windows;
    private final AugurService augur;
    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;

    @GetMapping
    public SearchedPlayerDto page(@PathVariable String riotId,
                                  @RequestParam(required = false) Integer days,
                                  @RequestParam(required = false) Integer patches,
                                  @RequestParam(required = false) Integer champions,
                                  @RequestParam(defaultValue = "false") boolean light) {
        return players.page(riotId, windows.days(days, patches), champions, light);
    }

    @GetMapping("/games")
    public MyGamesDto games(@PathVariable String riotId,
                            @RequestParam(required = false) Integer days,
                            @RequestParam(required = false) Integer patches,
                            @RequestParam(required = false) Integer limit) {
        return players.games(riotId, windows.days(days, patches), limit);
    }

    @GetMapping("/games/{matchId}")
    public TeamGameDetailDto game(@PathVariable String riotId,
                                  @PathVariable String matchId,
                                  @RequestParam(required = false) Integer days,
                                  @RequestParam(required = false) Integer patches) {
        return players.game(riotId, matchId, windows.days(days, patches));
    }

    // Page vue sans compte : seulement les constats neutres (le style), jamais les faiblesses d'un tiers.
    @GetMapping("/findings")
    public List<FindingDto> findings(@PathVariable String riotId,
                                     @RequestParam(required = false) Integer days,
                                     @RequestParam(required = false) Integer patches) {
        return augur.habit(players.resolve(riotId).puuid(), windows.days(days, patches),
                AugurService.Visibility.NEUTRAL);
    }

    @GetMapping("/games/{matchId}/findings")
    public List<FindingDto> gameFindings(@PathVariable String riotId, @PathVariable String matchId) {
        return augur.game(matchId, players.resolve(riotId).puuid(), AugurService.Visibility.NEUTRAL);
    }

    // La trace complète du moteur, pour régler les seuils : section « Débogage des calculs » de Schub.
    @GetMapping("/trace")
    public List<Evaluator.Evaluation> trace(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String riotId,
            @RequestParam(required = false) String matchId,
            @RequestParam(required = false) Integer days) {
        permissionEvaluator.require(userService.requireActor(actorId), Permission.INGEST_MANAGE, null);
        String puuid = players.resolve(riotId).puuid();
        return matchId == null || matchId.isBlank()
                ? augur.trace(PatternVersion.Scope.HABIT, puuid, null, days)
                : augur.trace(PatternVersion.Scope.GAME, puuid, matchId, null);
    }

    @PostMapping("/collect")
    public PlayerCollectDto collect(@PathVariable String riotId,
                                    @RequestHeader(value = VISITOR_ID, required = false) String visitor) {
        return players.collect(riotId, visitor);
    }
}
