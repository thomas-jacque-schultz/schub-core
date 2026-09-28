package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.IngestPauseDto;
import schultz.thomas.schub.core.team.api.dto.IngestPauseRequest;
import schultz.thomas.schub.core.team.api.dto.RiotDataInvalidationDto;
import schultz.thomas.schub.core.team.api.dto.RiotDataInvalidationRequest;
import schultz.thomas.schub.core.team.api.dto.RiotDataInventoryDto;
import schultz.thomas.schub.core.team.business.service.RiotDataInvalidationService;

// ROLE_MANAGE n'est jamais attribuable : seul l'OWNER invalide les données Riot.
@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
public class RiotDataController {

    private final RiotDataInvalidationService invalidation;
    private final PermissionEvaluator permissionEvaluator;
    private final UserService userService;

    @GetMapping("/pause")
    public IngestPauseDto pause(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        permissionEvaluator.require(userService.requireActor(actorId), Permission.INGEST_VIEW, null);
        return invalidation.pause();
    }

    @PutMapping("/pause")
    public IngestPauseDto updatePause(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                      @RequestBody IngestPauseRequest request) {
        permissionEvaluator.require(userService.requireActor(actorId), Permission.INGEST_MANAGE, null);
        return invalidation.setPause(request.paused());
    }

    @GetMapping("/riot-data")
    public RiotDataInventoryDto inventory(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        permissionEvaluator.require(userService.requireActor(actorId), Permission.ROLE_MANAGE, null);
        return invalidation.inventory();
    }

    @PostMapping("/riot-data/invalidate")
    public RiotDataInvalidationDto invalidate(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                              @RequestBody RiotDataInvalidationRequest request) {
        User actor = userService.requireActor(actorId);
        permissionEvaluator.require(actor, Permission.ROLE_MANAGE, null);
        return invalidation.invalidate(actor, request.confirmation());
    }
}
