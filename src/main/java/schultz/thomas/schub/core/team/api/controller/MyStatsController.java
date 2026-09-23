package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.business.service.MyStatsService;

// Jamais de route portant un puuid : il suffirait d'un puuid croisé ailleurs pour sonder l'historique de n'importe qui.
@RestController
@RequestMapping("/me/stats")
@RequiredArgsConstructor
public class MyStatsController {

    private final MyStatsService myStatsService;
    private final UserService userService;

    @GetMapping
    public MyStatsDto mine(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer champions) {
        return myStatsService.of(userService.requireActor(actorDiscordId), days, champions);
    }
}
