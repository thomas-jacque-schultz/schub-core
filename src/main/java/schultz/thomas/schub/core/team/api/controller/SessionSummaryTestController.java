package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.service.SessionSummaryService;

import java.time.LocalDate;
import java.util.List;

// Envoie à soi-même le résumé de session d'un jour récent, pour tester le MP.
@RestController
@RequestMapping("/premadelab/session-summary-test")
@RequiredArgsConstructor
public class SessionSummaryTestController {

    private final SessionSummaryService sessionSummaries;
    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;

    @GetMapping
    public List<SessionSummaryService.RecentSession> recent(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam(defaultValue = "30") int days) {
        User actor = autorise(actorId);
        return sessionSummaries.recentFor(actor, Math.clamp(days, 1, 90));
    }

    @PostMapping
    public ResponseEntity<SessionSummaryService.TestResult> send(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam LocalDate day,
            @RequestParam(defaultValue = "true") boolean send) {
        return sessionSummaries.sendTest(autorise(actorId), day, send)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private User autorise(String actorId) {
        User actor = userService.requireActor(actorId);
        permissionEvaluator.require(actor, Permission.INGEST_MANAGE, null);
        return actor;
    }
}
