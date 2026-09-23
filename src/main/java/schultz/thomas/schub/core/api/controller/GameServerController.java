package schultz.thomas.schub.core.api.controller;

import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.PublicServerStatusDto;
import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.DeploymentService;
import schultz.thomas.schub.core.business.service.GameServerProjectionService;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Game;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/game-servers")
@RequiredArgsConstructor
public class GameServerController {

    private final GameServerService gameServerService;
    private final DeploymentService deploymentService;
    private final GameServerProjectionService projectionService;
    private final PermissionEvaluator permissionEvaluator;
    private final UserService userService;
    private final GameServerMapper mapper;

    @GetMapping
    public List<?> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        Optional<User> actor = readActor(actorDiscordId);
        actor.ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return projectionService.project(gameServerService.findAll(), actor.orElse(null));
    }

    @GetMapping("/public-status")
    public List<PublicServerStatusDto> publicStatus() {
        return gameServerService.findAll().stream()
                .map(server -> new PublicServerStatusDto(
                        server.getName(),
                        server.getGame() != null ? server.getGame().getLabel() : null,
                        server.getStatus() != null ? server.getStatus().name() : null))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> byId(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                                  @PathVariable String id) {
        Optional<User> actor = readActor(actorDiscordId);
        actor.ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return gameServerService.findById(id)
                .map(server -> projectionService.project(server, actor.orElse(null)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GameServerDto> create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody GameServerDto dto) {
        User actor = userService.requireActor(actorDiscordId);
        permissionEvaluator.require(actor, Permission.SERVER_CREATE, null);
        GameServer created = gameServerService.create(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(projectionService.toInfraDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameServerDto> update(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String id,
            @RequestBody GameServerDto dto) {
        User actor = userService.requireActor(actorDiscordId);
        permissionEvaluator.require(actor, Permission.SERVER_EDIT, null);
        GameServer updated = gameServerService.update(id, mapper.toEntity(dto));
        return ResponseEntity.ok(projectionService.toInfraDto(updated));
    }

    @PostMapping("/{slug}/start")
    public ResponseEntity<Void> start(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String slug) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId),
                Permission.SERVER_START, null);
        deploymentService.start(gameServerService.requireBySlug(slug));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{slug}/stop")
    public ResponseEntity<Void> stop(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String slug) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId),
                Permission.SERVER_STOP, null);
        deploymentService.stop(gameServerService.requireBySlug(slug));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/games")
    public List<GameDto> games(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        readActor(actorDiscordId).ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return java.util.Arrays.stream(Game.values())
                .map(game -> new GameDto(game.name(), game.getLabel(), game.getIconUrl()))
                .toList();
    }

    private Optional<User> readActor(String actorDiscordId) {
        if (actorDiscordId == null || actorDiscordId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(userService.requireActor(actorDiscordId));
    }

    public record GameDto(String name, String label, String iconUrl) {
    }
}
