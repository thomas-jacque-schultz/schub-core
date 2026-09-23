package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.RoleDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RoleService;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;

    @GetMapping
    public List<RoleDto> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        User actor = userService.requireActor(actorDiscordId);
        if (!permissionEvaluator.can(actor, Permission.USER_VIEW, null)
                && !permissionEvaluator.can(actor, Permission.ROLE_MANAGE, null)) {
            throw new AccessDeniedException("Permission USER_VIEW ou ROLE_MANAGE requise");
        }
        return roleService.findAll().stream().map(roleService::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<RoleDto> create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody RoleDto body) {
        requireRoleManage(actorDiscordId);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.toDto(roleService.create(body)));
    }

    @PutMapping("/{id}")
    public RoleDto update(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                          @PathVariable String id,
                          @RequestBody RoleDto body) {
        requireRoleManage(actorDiscordId);
        return roleService.toDto(roleService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String id) {
        requireRoleManage(actorDiscordId);
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireRoleManage(String actorDiscordId) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.ROLE_MANAGE, null);
    }
}
