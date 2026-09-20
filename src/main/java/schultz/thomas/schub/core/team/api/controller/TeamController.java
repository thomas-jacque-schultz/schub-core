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
import schultz.thomas.schub.core.team.api.dto.AddMemberRequest;
import schultz.thomas.schub.core.team.api.dto.TeamDto;
import schultz.thomas.schub.core.team.api.dto.TeamNameRequest;
import schultz.thomas.schub.core.team.api.dto.TeamSummaryDto;
import schultz.thomas.schub.core.team.api.dto.UpdateMemberRequest;
import schultz.thomas.schub.core.team.business.service.TeamProjectionService;
import schultz.thomas.schub.core.team.business.service.TeamService;
import schultz.thomas.schub.core.team.data.model.Team;

import java.util.List;

/**
 * L'API des équipes.
 *
 * <p><strong>Un acteur est obligatoire sur toutes les routes</strong>, sans l'exception que se
 * permettent les serveurs de jeu : une équipe n'a pas de vue publique, et aucun service Schub
 * n'a de raison d'en lire une pour son propre compte. En-tête absent ou inconnu vaut refus.</p>
 *
 * <p>Les permissions sont évaluées au point d'action, avec l'équipe comme ressource. Appartenir à
 * une équipe y donne des droits, exactement comme figurer dans les {@code admins} d'un serveur
 * en donne sur celui-là (plan §A.1).</p>
 */
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamProjectionService projectionService;
    private final UserService userService;

    /**
     * Les équipes de l'appelant — celles où il figure, et celles qu'il a créées.
     *
     * <p>C'est la route qui réalise « les joueurs ajoutés voient l'équipe apparaître sur leur
     * compte » : rien de plus n'est nécessaire côté front qu'appeler celle-ci.</p>
     */
    @GetMapping
    public List<TeamSummaryDto> mine(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toSummaries(teamService.mine(actor), actor);
    }

    @GetMapping("/{teamId}")
    public TeamDto byId(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                        @PathVariable String teamId) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDto(teamService.requireVisible(actor, teamId), actor);
    }

    @PostMapping
    public ResponseEntity<TeamDto> create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody TeamNameRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        Team created = teamService.create(actor, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(projectionService.toDto(created, actor));
    }

    @PutMapping("/{teamId}")
    public TeamDto rename(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                          @PathVariable String teamId,
                          @RequestBody TeamNameRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDto(teamService.rename(actor, teamId, request.name()), actor);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId) {
        teamService.delete(userService.requireActor(actorDiscordId), teamId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamDto> addMember(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestBody AddMemberRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        Team team = teamService.addMember(actor, teamId, new TeamService.NewMember(
                request.riotGameName(), request.riotTagLine(), request.riotPuuid(),
                request.role(), request.status()));
        return ResponseEntity.status(HttpStatus.CREATED).body(projectionService.toDto(team, actor));
    }

    @PutMapping("/{teamId}/members/{memberId}")
    public TeamDto updateMember(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String memberId,
            @RequestBody UpdateMemberRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDto(
                teamService.updateMember(actor, teamId, memberId, request.role(), request.status()), actor);
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public TeamDto removeMember(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String memberId) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toDto(teamService.removeMember(actor, teamId, memberId), actor);
    }

    /**
     * « Ces places m'attendaient » — à appeler après la liaison du compte Riot (lot D.3).
     *
     * <p>Rend les équipes qui viennent de basculer, donc vides si rien n'attendait l'appelant.
     * Idempotente : la rappeler ne relie rien de plus.</p>
     */
    @PostMapping("/claim")
    public List<TeamSummaryDto> claim(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        User actor = userService.requireActor(actorDiscordId);
        return projectionService.toSummaries(teamService.claim(actor), actor);
    }
}
