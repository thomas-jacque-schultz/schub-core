package schultz.thomas.schub.core.api.controller;

import schultz.thomas.schub.core.api.dto.PortainerStack;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.ContainerRequestService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentCatalogController {

    @Qualifier("portainerRequestService")
    private final ContainerRequestService containerRequestService;

    private final PermissionEvaluator permissionEvaluator;

    private final UserService userService;

    @GetMapping
    public List<PortainerStack> all(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.SERVER_INFRA_VIEW, null);
        return containerRequestService.listStacks();
    }
}
