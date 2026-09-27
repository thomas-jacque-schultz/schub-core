package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.PremadeLabSettingsDto;
import schultz.thomas.schub.core.team.business.service.PremadeLabSettingsService;

@RestController
@RequestMapping("/premadelab/settings")
@RequiredArgsConstructor
public class PremadeLabSettingsController {

    private final PremadeLabSettingsService settings;
    private final UserService userService;

    @GetMapping
    public PremadeLabSettingsDto get(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        return settings.view(userService.requireActor(actorId));
    }

    @PutMapping
    public PremadeLabSettingsDto update(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                        @RequestBody PremadeLabSettingsDto request) {
        return settings.update(userService.requireActor(actorId), request);
    }
}
