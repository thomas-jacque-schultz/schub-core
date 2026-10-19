package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.PortainerStackDto;
import schultz.thomas.schub.core.api.mapper.PortForwardingDtoMapper;
import schultz.thomas.schub.core.business.service.ContainerRequestService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Permission;

import java.util.List;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentCatalogController {

    @Qualifier("portainerRequestService")
    private final ContainerRequestService containerRequestService;

    private final PermissionEvaluator permissionEvaluator;

    private final UserService userService;

    private final PortForwardingDtoMapper dtoMapper;

    @GetMapping
    public List<PortainerStackDto> all(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        permissionEvaluator.require(userService.requireActor(actorId), Permission.SERVER_INFRA_VIEW, null);
        return dtoMapper.toStackDtos(containerRequestService.listStacks());
    }
}
