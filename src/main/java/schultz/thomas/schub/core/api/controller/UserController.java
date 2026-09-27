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
import java.util.NoSuchElementException;
import java.util.Set;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;
    private final RiotAccountService riotAccountService;

    @GetMapping
    public List<UserDto> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        User actor = userService.requireActor(actorId);
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

    @GetMapping("/{id}/identity")
    public UserIdentityDto identity(@PathVariable String id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aucun utilisateur d'identifiant '" + id + "'"));
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
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        return riotAccountService.of(userService.requireActor(actorId));
    }

    @PutMapping("/me/riot-account")
    public RiotAccountDto linkMyRiotAccount(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestBody RiotAccountRequest request) {
        return riotAccountService.link(userService.requireActor(actorId),
                request.riotId(), request.confirmChange());
    }

    @GetMapping("/me/riot-account/suggestions")
    public List<RiotAccountSuggestionDto> suggestRiotAccounts(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return riotAccountService.suggestions(userService.requireActor(actorId), q, limit);
    }


    @PutMapping("/{id}/role")
    public UserDto assignRole(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                              @PathVariable String id,
                              @RequestBody AssignRoleRequest request) {
        User actor = userService.requireActor(actorId);
        return userService.toDto(userService.assignRole(actor, id, request.roleId()));
    }
}
