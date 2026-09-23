package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.DisplayNameRequest;
import schultz.thomas.schub.core.api.dto.MeDto;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public MeDto me(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        return userService.toMeDto(userService.requireActor(actorDiscordId));
    }

    @PutMapping("/display-name")
    public MeDto changeDisplayName(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody DisplayNameRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return userService.toMeDto(userService.changeDisplayName(actor, request.displayName()));
    }
}
