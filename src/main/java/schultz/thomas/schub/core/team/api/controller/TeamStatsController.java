package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.StatsRefreshDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamOppositionDto;
import schultz.thomas.schub.core.team.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.team.business.service.TeamGamesStatsService;
import schultz.thomas.schub.core.team.business.service.TeamOppositionService;
import schultz.thomas.schub.core.team.business.service.TeamPlayerStatsService;
import schultz.thomas.schub.core.team.business.service.TeamStatsRefreshService;
import schultz.thomas.schub.core.team.business.service.StatsWindows;

@RestController
@RequestMapping("/teams/{teamId}/stats")
@RequiredArgsConstructor
public class TeamStatsController {

    private final TeamPlayerStatsService playersStats;
    private final TeamGamesStatsService gamesStats;
    private final TeamOppositionService opposition;
    private final TeamStatsRefreshService refresh;
    private final UserService userService;
    private final StatsWindows windows;

    @GetMapping("/refresh")
    public StatsRefreshDto refreshStatus(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId) {
        return refresh.status(userService.requireActor(actorDiscordId), teamId);
    }

    @PostMapping("/refresh")
    public StatsRefreshDto refresh(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId) {
        return refresh.refresh(userService.requireActor(actorDiscordId), teamId);
    }

    @GetMapping("/players")
    public TeamPlayersStatsDto players(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches,
            @RequestParam(required = false) Integer champions) {
        return playersStats.of(userService.requireActor(actorDiscordId), teamId, windows.days(days, patches), champions);
    }

    @GetMapping("/team")
    public TeamGamesStatsDto team(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches,
            @RequestParam(required = false) Integer limit) {
        return gamesStats.of(userService.requireActor(actorDiscordId), teamId, windows.days(days, patches), limit);
    }

    @GetMapping("/games/{matchId}")
    public TeamGameDetailDto game(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String matchId) {
        return gamesStats.detail(userService.requireActor(actorDiscordId), teamId, matchId);
    }

    @GetMapping("/opposition")
    public TeamOppositionDto opposition(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches) {
        return opposition.of(userService.requireActor(actorDiscordId), teamId, windows.days(days, patches));
    }
}
