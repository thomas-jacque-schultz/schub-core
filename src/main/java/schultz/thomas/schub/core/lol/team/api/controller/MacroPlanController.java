package schultz.thomas.schub.core.lol.team.api.controller;

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
import schultz.thomas.schub.core.lol.team.api.dto.MacroPlanDto;
import schultz.thomas.schub.core.lol.team.api.dto.MacroPlanRequest;
import schultz.thomas.schub.core.lol.team.api.dto.MacroPlanSummaryDto;
import schultz.thomas.schub.core.lol.team.api.mapper.TeamDtoMapper;
import schultz.thomas.schub.core.lol.team.business.service.MacroPlanService;

import java.util.List;

@RestController
@RequestMapping("/teams/{teamId}/macro-plans")
@RequiredArgsConstructor
public class MacroPlanController {

    private final MacroPlanService service;
    private final UserService userService;
    private final TeamDtoMapper mapper;

    @GetMapping
    public List<MacroPlanSummaryDto> macroPlans(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId) {
        return mapper.toMacroPlanSummaries(service.ofTeam(userService.requireActor(actorId), teamId));
    }

    @GetMapping("/{planId}")
    public MacroPlanDto macroPlan(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @PathVariable String planId) {
        return mapper.toDto(service.require(userService.requireActor(actorId), teamId, planId));
    }

    @PostMapping
    public ResponseEntity<MacroPlanDto> createMacroPlan(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @RequestBody MacroPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(
                service.create(userService.requireActor(actorId), teamId, request.name(), request.elements())));
    }

    @PutMapping("/{planId}")
    public MacroPlanDto updateMacroPlan(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @PathVariable String planId,
            @RequestBody MacroPlanRequest request) {
        return mapper.toDto(service.update(userService.requireActor(actorId), teamId, planId, request.name(),
                request.elements()));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deleteMacroPlan(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @PathVariable String planId) {
        service.delete(userService.requireActor(actorId), teamId, planId);
        return ResponseEntity.noContent().build();
    }
}
