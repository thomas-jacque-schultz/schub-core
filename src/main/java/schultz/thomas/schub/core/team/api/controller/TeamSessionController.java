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
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.service.SessionWatcher;
import schultz.thomas.schub.core.team.business.service.TeamService;
import schultz.thomas.schub.core.team.data.model.TeamSession;
import schultz.thomas.schub.core.team.data.repository.TeamSessionRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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

    @GetMapping
    public List<TeamSession> list(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                  @PathVariable String teamId,
                                  @RequestParam(defaultValue = "60") int days) {
        teamService.requireVisible(userService.requireActor(actorId), teamId);
        return sessions.findByTeamIdAndDayGreaterThanEqualOrderByStartedAtDesc(teamId,
                LocalDate.now(PARIS).minusDays(Math.clamp(days, 1, 366)));
    }

    // Relance la détection d'une journée passée : l'observateur ne repasse pas de lui-même.
    @PostMapping("/detect")
    public List<TeamSession> detect(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                    @PathVariable String teamId,
                                    @RequestParam LocalDate day) {
        User actor = userService.requireActor(actorId);
        permissionEvaluator.require(actor, Permission.TEAM_MANAGE, TeamService.ref(teamId));
        return watcher.detect(teamService.require(teamId), day);
    }

}
