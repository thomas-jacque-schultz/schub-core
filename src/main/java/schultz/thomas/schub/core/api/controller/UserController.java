package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.AssignRoleRequest;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.api.dto.RiotAccountRequest;
import schultz.thomas.schub.core.api.dto.RiotAccountSuggestionDto;
import schultz.thomas.schub.core.api.dto.UserDto;
import schultz.thomas.schub.core.api.dto.UserIdentityDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotAccountService;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;
    private final RiotAccountService riotAccountService;

    @GetMapping
    public List<UserDto> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        User actor = userService.requireActor(actorDiscordId);
        permissionEvaluator.require(actor, Permission.USER_VIEW, null);
        return userService.toDtos(userService.findAll());
    }

    @GetMapping("/by-discord/{discordId}")
    public UserIdentityDto byDiscordId(@PathVariable String discordId,
                                       @RequestParam(required = false) String discordUsername,
                                       @RequestParam(required = false) String avatarUrl) {
        User user = userService.findOrCreateByDiscordId(discordId, discordUsername, avatarUrl);
        riotAccountService.resolvePendingLink(user);
        return userService.toIdentityDto(user);
    }

    @GetMapping("/by-discord/{discordId}/permissions")
    public Set<Permission> effectivePermissions(@PathVariable String discordId,
                                                @RequestParam(required = false) String discordUsername) {
        User user = userService.findOrCreateByDiscordId(discordId, discordUsername, null);
        return permissionEvaluator.rolePermissions(user);
    }

    @GetMapping("/me/riot-account")
    public RiotAccountDto myRiotAccount(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        return riotAccountService.of(userService.requireActor(actorDiscordId));
    }

    @PutMapping("/me/riot-account")
    public RiotAccountDto linkMyRiotAccount(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody RiotAccountRequest request) {
        return riotAccountService.link(userService.requireActor(actorDiscordId),
                request.riotId(), request.confirmChange());
    }

    @GetMapping("/me/riot-account/suggestions")
    public List<RiotAccountSuggestionDto> suggestRiotAccounts(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return riotAccountService.suggestions(userService.requireActor(actorDiscordId), q, limit);
    }


    @PutMapping("/{id}/role")
    public UserDto assignRole(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                              @PathVariable String id,
                              @RequestBody AssignRoleRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return userService.toDto(userService.assignRole(actor, id, request.roleId()));
    }
}
