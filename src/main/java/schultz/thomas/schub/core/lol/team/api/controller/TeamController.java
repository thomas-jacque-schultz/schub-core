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
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.team.api.dto.AddMemberRequest;
import schultz.thomas.schub.core.lol.team.api.dto.TeamDto;
import schultz.thomas.schub.core.lol.team.api.dto.TeamNameRequest;
import schultz.thomas.schub.core.lol.team.api.dto.TeamSummaryDto;
import schultz.thomas.schub.core.lol.team.api.dto.UpdateMemberRequest;
import schultz.thomas.schub.core.lol.team.api.mapper.TeamProjection;
import schultz.thomas.schub.core.lol.team.business.service.TeamService;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.model.TrainingSlot;

import java.util.List;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamProjection projection;
    private final UserService userService;

    @GetMapping
    public List<TeamSummaryDto> mine(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        User actor = userService.requireActor(actorId);
        return projection.toSummaries(teamService.mine(actor), actor);
    }

    @GetMapping("/{teamId}")
    public TeamDto byId(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                        @PathVariable String teamId) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(teamService.requireVisible(actor, teamId), actor);
    }

    @PostMapping
    public ResponseEntity<TeamDto> create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestBody TeamNameRequest request) {
        User actor = userService.requireActor(actorId);
        Team created = teamService.create(actor, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(projection.toDto(created, actor));
    }

    @PutMapping("/{teamId}")
    public TeamDto rename(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                          @PathVariable String teamId,
                          @RequestBody TeamNameRequest request) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(teamService.rename(actor, teamId, request.name()), actor);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId) {
        teamService.delete(userService.requireActor(actorId), teamId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamDto> addMember(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @RequestBody AddMemberRequest request) {
        User actor = userService.requireActor(actorId);
        Team team = teamService.addMember(actor, teamId, new TeamService.NewMember(
                request.riotGameName(), request.riotTagLine(), request.riotPuuid(),
                request.roles(), request.status(), Boolean.TRUE.equals(request.coach())));
        return ResponseEntity.status(HttpStatus.CREATED).body(projection.toDto(team, actor));
    }

    @PutMapping("/{teamId}/members/{memberId}")
    public TeamDto updateMember(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @PathVariable String memberId,
            @RequestBody UpdateMemberRequest request) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(
                teamService.updateMember(actor, teamId, memberId, request.roles(), request.status(),
                        request.coach()), actor);
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public TeamDto removeMember(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @PathVariable String memberId) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(teamService.removeMember(actor, teamId, memberId), actor);
    }

    @PutMapping("/{teamId}/public")
    public TeamDto setPublic(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                             @PathVariable String teamId,
                             @RequestBody VisibilityRequest request) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(teamService.setPublicPage(actor, teamId, request.value()), actor);
    }

    @PutMapping("/{teamId}/members/{memberId}/hidden")
    public TeamDto setMemberHidden(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                   @PathVariable String teamId,
                                   @PathVariable String memberId,
                                   @RequestBody VisibilityRequest request) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(teamService.setMemberHidden(actor, teamId, memberId, request.value()), actor);
    }

    public record VisibilityRequest(boolean value) {
    }

    @PutMapping("/{teamId}/training-slots")
    public TeamDto setTrainingSlots(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                    @PathVariable String teamId,
                                    @RequestBody List<TrainingSlot> slots) {
        User actor = userService.requireActor(actorId);
        return projection.toDto(teamService.setTrainingSlots(actor, teamId, slots), actor);
    }

    @PostMapping("/claim")
    public List<TeamSummaryDto> claim(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        User actor = userService.requireActor(actorId);
        return projection.toSummaries(teamService.claim(actor), actor);
    }
}
