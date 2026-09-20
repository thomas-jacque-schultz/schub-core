package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.CompositionDto;
import schultz.thomas.schub.core.team.api.dto.CompositionRequest;
import schultz.thomas.schub.core.team.api.dto.CompositionSlotRequest;
import schultz.thomas.schub.core.team.business.service.CompositionService;
import schultz.thomas.schub.core.team.business.service.TeamProjectionService;
import schultz.thomas.schub.core.team.business.service.TeamService;
import schultz.thomas.schub.core.team.data.model.CompositionSlot;

import java.util.List;

/**
 * Les compositions d'une équipe — le socle du futur préparateur de draft (lot D.6).
 *
 * <p>Sous {@code /teams/{teamId}} parce qu'une composition n'existe pas sans son équipe : c'est
 * elle qui porte les droits, et une route à plat obligerait à retrouver l'équipe pour savoir qui
 * a le droit d'écrire.</p>
 */
@RestController
@RequestMapping("/teams/{teamId}/compositions")
@RequiredArgsConstructor
public class CompositionController {

    private final CompositionService compositionService;
    private final TeamService teamService;
    private final TeamProjectionService projectionService;
    private final UserService userService;

    @GetMapping
    public List<CompositionDto> all(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDtos(
                compositionService.ofTeam(actor, teamId), teamService.require(teamId), actor);
    }

    @GetMapping("/{compositionId}")
    public CompositionDto byId(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String compositionId) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDto(
                compositionService.require(actor, teamId, compositionId), teamService.require(teamId), actor);
    }

    @PostMapping
    public ResponseEntity<CompositionDto> create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestBody CompositionRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        CompositionDto dto = projectionService.toDto(
                compositionService.create(actor, teamId, toDraft(request)), teamService.require(teamId), actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{compositionId}")
    public CompositionDto update(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String compositionId,
            @RequestBody CompositionRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDto(
                compositionService.update(actor, teamId, compositionId, toDraft(request)),
                teamService.require(teamId), actor);
    }

    @DeleteMapping("/{compositionId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String compositionId) {
        compositionService.delete(userService.requireActor(actorDiscordId), teamId, compositionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Traduit la demande en brouillon du domaine. Aucune validation ici : la cardinalité et la
     * couverture des postes sont des règles de domaine, et les vérifier dans un contrôleur
     * reviendrait à pouvoir les contourner par un autre appelant.
     */
    private CompositionService.Draft toDraft(CompositionRequest request) {
        List<CompositionSlot> slots = request.slots() == null ? null
                : request.slots().stream()
                .map(slot -> slot == null ? null
                        : new CompositionSlot(slot.role(), slot.championId(), slot.memberId()))
                .toList();
        return new CompositionService.Draft(request.name(), slots, request.patch(), request.notes());
    }
}
