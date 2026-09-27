package schultz.thomas.schub.core.augur.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.augur.api.dto.ImpactDto;
import schultz.thomas.schub.core.augur.api.dto.PatternDto;
import schultz.thomas.schub.core.augur.api.dto.PatternRequest;
import schultz.thomas.schub.core.augur.business.service.PatternService;
import schultz.thomas.schub.core.business.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/augur/patterns")
@RequiredArgsConstructor
public class PatternController {

    public record CommentRequest(String comment) {
    }

    private final PatternService patterns;
    private final UserService userService;

    @GetMapping
    public List<PatternDto> list(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        return patterns.list(userService.requireActor(actorId));
    }

    @GetMapping("/{key}/versions")
    public List<PatternDto> history(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                    @PathVariable String key) {
        return patterns.history(userService.requireActor(actorId), key);
    }

    @PostMapping
    public PatternDto draft(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                            @RequestBody PatternRequest request) {
        return patterns.draft(userService.requireActor(actorId), request);
    }

    @GetMapping("/{key}/versions/{version}/impact")
    public ImpactDto impact(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                            @PathVariable String key, @PathVariable int version) {
        return patterns.impact(userService.requireActor(actorId), key, version);
    }

    @PostMapping("/{key}/versions/{version}/activate")
    public PatternDto activate(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                               @PathVariable String key, @PathVariable int version,
                               @RequestBody CommentRequest request) {
        return patterns.activate(userService.requireActor(actorId), key, version, request.comment());
    }

    @PostMapping("/{key}/rollback")
    public PatternDto rollback(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                               @PathVariable String key, @RequestBody CommentRequest request) {
        return patterns.rollback(userService.requireActor(actorId), key, request.comment());
    }
}
