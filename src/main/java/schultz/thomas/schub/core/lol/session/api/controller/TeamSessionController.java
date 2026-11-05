package schultz.thomas.schub.core.lol.session.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.session.api.dto.TeamSessionDto;
import schultz.thomas.schub.core.lol.session.api.mapper.SessionSummaryDtoMapper;
import schultz.thomas.schub.core.lol.session.business.service.SessionWatcher;
import schultz.thomas.schub.core.lol.session.data.repository.TeamSessionRepository;
import schultz.thomas.schub.core.lol.session.data.model.TeamSession;
import schultz.thomas.schub.core.lol.stats.api.dto.TeamGameDto;
import schultz.thomas.schub.core.lol.stats.api.mapper.StatsDtoMapper;
import schultz.thomas.schub.core.lol.stats.business.service.TeamGamesStatsService;
import schultz.thomas.schub.core.lol.team.business.service.TeamService;
import schultz.thomas.schub.core.lol.team.data.model.Team;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/teams/{teamId}/sessions")
@RequiredArgsConstructor
public class TeamSessionController {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final TeamService teamService;
    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;
    private final TeamSessionRepository sessions;
    private final SessionWatcher watcher;
    private final SessionSummaryDtoMapper dtos;
    private final TeamGamesStatsService gamesStats;
    private final StatsDtoMapper statsDtos;

    @GetMapping
    public List<TeamSessionDto> list(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                  @PathVariable String teamId,
                                  @RequestParam(defaultValue = "60") int days) {
        teamService.requireVisible(userService.requireActor(actorId), teamId);
        return dtos.toSessionDtos(sessions.findByTeamIdAndDayGreaterThanEqualOrderByStartedAtDesc(teamId,
                LocalDate.now(PARIS).minusDays(Math.clamp(days, 1, 366))));
    }

    // L'identifiant d'une session contient « | » : il passe en paramètre, pas dans le chemin.
    @GetMapping("/games")
    public List<TeamGameDto> games(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                   @PathVariable String teamId,
                                   @RequestParam String session) {
        Team team = teamService.requireVisible(userService.requireActor(actorId), teamId);
        TeamSession lue = sessions.findById(session).filter(s -> teamId.equals(s.teamId()))
                .orElseThrow(() -> new NoSuchElementException("Aucune session « " + session + " »"));
        return statsDtos.toGameDtos(gamesStats.partiesDeSession(team, lue.matchIds()));
    }

    // Relance la détection d'une journée passée : l'observateur ne repasse pas de lui-même.
    @PostMapping("/detect")
    public List<TeamSessionDto> detect(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                    @PathVariable String teamId,
                                    @RequestParam LocalDate day) {
        User actor = userService.requireActor(actorId);
        permissionEvaluator.require(actor, Permission.TEAM_MANAGE, TeamService.ref(teamId));
        return dtos.toSessionDtos(watcher.detect(teamService.require(teamId), day));
    }

}
