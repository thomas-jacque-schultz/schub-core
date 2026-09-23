package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.team.business.service.TeamGamesStatsService;
import schultz.thomas.schub.core.team.business.service.TeamPlayerStatsService;

@RestController
@RequestMapping("/teams/{teamId}/stats")
@RequiredArgsConstructor
public class TeamStatsController {

    private final TeamPlayerStatsService playersStats;
    private final TeamGamesStatsService gamesStats;
    private final UserService userService;

    @GetMapping("/players")
    public TeamPlayersStatsDto players(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer champions) {
        return playersStats.of(userService.requireActor(actorDiscordId), teamId, days, champions);
    }

    @GetMapping("/team")
    public TeamGamesStatsDto team(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit) {
        return gamesStats.of(userService.requireActor(actorDiscordId), teamId, days, limit);
    }
}
