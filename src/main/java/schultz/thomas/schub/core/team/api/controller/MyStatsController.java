package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.augur.api.dto.FindingDto;
import schultz.thomas.schub.core.augur.business.service.AugurService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.service.MyGamesService;
import schultz.thomas.schub.core.team.business.service.MyStatsService;
import schultz.thomas.schub.core.team.business.service.StatsWindows;

// Jamais de route portant un puuid : il suffirait d'un puuid croisé ailleurs pour sonder l'historique de n'importe qui.
@RestController
@RequestMapping("/me/stats")
@RequiredArgsConstructor
public class MyStatsController {

    private final MyStatsService myStatsService;
    private final MyGamesService myGames;
    private final UserService userService;
    private final StatsWindows windows;
    private final AugurService augur;

    @GetMapping
    public MyStatsDto mine(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches,
            @RequestParam(required = false) Integer champions) {
        return myStatsService.of(userService.requireActor(actorId), windows.days(days, patches), champions);
    }

    @GetMapping("/games")
    public MyGamesDto games(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches,
            @RequestParam(required = false) Integer limit) {
        return myGames.games(userService.requireActor(actorId), windows.days(days, patches), limit);
    }

    // Ses constats d'habitude : forces, faiblesses et style, avec leur preuve.
    @GetMapping("/findings")
    public List<FindingDto> findings(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches) {
        User moi = userService.requireActor(actorId);
        return moi.getRiotPuuid() == null ? List.of()
                : augur.habit(moi.getRiotPuuid(), windows.days(days, patches), AugurService.Visibility.ALL);
    }

    @GetMapping("/games/{matchId}/findings")
    public List<FindingDto> gameFindings(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String matchId) {
        User moi = userService.requireActor(actorId);
        return moi.getRiotPuuid() == null ? List.of()
                : augur.game(matchId, moi.getRiotPuuid(), AugurService.Visibility.ALL);
    }

    @GetMapping("/games/{matchId}")
    public TeamGameDetailDto game(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String matchId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches) {
        return myGames.game(userService.requireActor(actorId), matchId, windows.days(days, patches));
    }
}
