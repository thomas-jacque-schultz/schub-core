package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.PoolChampionsRequest;
import schultz.thomas.schub.core.team.api.dto.PoolMasteryFloorRequest;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.service.ChampionPoolService;

@RestController
@RequestMapping("/teams/{teamId}/champion-pool")
@RequiredArgsConstructor
public class ChampionPoolController {

    private final ChampionPoolService championPoolService;
    private final UserService userService;

    @GetMapping
    public ChampionPoolDto byTeam(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer masteryFloor) {
        return championPoolService.of(userService.requireActor(actorDiscordId), teamId, masteryFloor);
    }

    @PutMapping("/roles/{role}")
    public ChampionPoolDto setChampions(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable GameRole role,
            @RequestBody PoolChampionsRequest request) {
        return championPoolService.setChampions(userService.requireActor(actorDiscordId), teamId, role,
                request == null ? null : request.championKeys());
    }

    @PutMapping("/mastery-floor")
    public ChampionPoolDto setMasteryFloor(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestBody PoolMasteryFloorRequest request) {
        return championPoolService.setMasteryFloor(userService.requireActor(actorDiscordId), teamId,
                request == null ? 0 : request.masteryFloor());
    }
}
