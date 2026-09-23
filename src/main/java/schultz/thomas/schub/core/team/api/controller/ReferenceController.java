package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.team.business.service.RiotStatsGateway;

import java.util.Set;

// 204 : pas encore de référentiel pour ce poste (population trop mince, ou connecteur muet). L'écran note sans icône.
@RestController
@RequestMapping("/lol/references")
@RequiredArgsConstructor
public class ReferenceController {

    private static final Set<String> POSTES = Set.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");
    private static final Set<String> PORTEES = Set.of("GAME", "MEAN");

    private final RiotStatsGateway statsGateway;
    private final UserService userService;

    @GetMapping("/{position}")
    public ResponseEntity<ReferenceGridDto> grid(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String position,
            @RequestParam(defaultValue = "MEAN") String scope,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String patch) {
        userService.requireActor(actorDiscordId);
        if (!POSTES.contains(position) || !PORTEES.contains(scope)) {
            return ResponseEntity.badRequest().build();
        }
        return statsGateway.referenceGrid(position, scope, tier, patch)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
