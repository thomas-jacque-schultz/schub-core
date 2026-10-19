package schultz.thomas.schub.core.api.controller;

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
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.PublicServerStatusDto;
import schultz.thomas.schub.core.api.mapper.GameServerMapper;
import schultz.thomas.schub.core.api.mapper.GameServerProjection;
import schultz.thomas.schub.core.business.service.DeploymentService;
import schultz.thomas.schub.core.business.service.GameServerControlPolicy;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Game;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/game-servers")
@RequiredArgsConstructor
public class GameServerController {

    private final GameServerService gameServerService;
    private final DeploymentService deploymentService;
    private final GameServerProjection projection;
    private final PermissionEvaluator permissionEvaluator;
    private final UserService userService;
    private final GameServerMapper mapper;
    private final GameServerControlPolicy controlPolicy;

    @GetMapping
    public List<?> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        Optional<User> actor = readActor(actorId);
        actor.ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return projection.project(gameServerService.findAll(), actor.orElse(null));
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
    public ResponseEntity<?> byId(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                  @PathVariable String id) {
        Optional<User> actor = readActor(actorId);
        actor.ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return gameServerService.findById(id)
                .map(server -> projection.project(server, actor.orElse(null)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GameServerDto> create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestBody GameServerDto dto) {
        User actor = userService.requireActor(actorId);
        permissionEvaluator.require(actor, Permission.SERVER_CREATE, null);
        GameServer created = gameServerService.create(withKnownReferents(mapper.toEntity(dto)));
        return ResponseEntity.status(HttpStatus.CREATED).body(projection.toInfraDto(created, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameServerDto> update(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String id,
            @RequestBody GameServerDto dto) {
        User actor = userService.requireActor(actorId);
        permissionEvaluator.require(actor, Permission.SERVER_EDIT, null);
        GameServer updated = gameServerService.update(id, withKnownReferents(mapper.toEntity(dto)));
        return ResponseEntity.ok(projection.toInfraDto(updated, actor));
    }

    @PostMapping("/{slug}/start")
    public ResponseEntity<Void> start(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String slug) {
        GameServer server = gameServerService.requireBySlug(slug);
        controlPolicy.require(userService.requireActor(actorId), server, Permission.SERVER_START);
        deploymentService.start(server);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{slug}/stop")
    public ResponseEntity<Void> stop(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String slug) {
        GameServer server = gameServerService.requireBySlug(slug);
        controlPolicy.require(userService.requireActor(actorId), server, Permission.SERVER_STOP);
        deploymentService.stop(server);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/games")
    public List<GameDto> games(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        readActor(actorId).ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return java.util.Arrays.stream(Game.values())
                .map(game -> new GameDto(game.name(), game.getLabel(), game.getIconUrl()))
                .toList();
    }

    private GameServer withKnownReferents(GameServer server) {
        if (server.getReferents() != null) {
            server.setReferents(List.copyOf(userService.byIds(Set.copyOf(server.getReferents())).keySet()));
        }
        return server;
    }

    private Optional<User> readActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(userService.requireActor(actorId));
    }

    public record GameDto(String name, String label, String iconUrl) {
    }
}
