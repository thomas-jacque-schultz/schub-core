package schultz.thomas.schub.core.api.controller;

import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.PublicServerStatusDto;
import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
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

/**
 * L'API du domaine. Tout ce qui concerne un serveur passe par ici — le BFF, le connecteur
 * Discord qui tire périodiquement, et rien d'autre.
 *
 * <p>Depuis le 18-09, chaque route interroge le {@link PermissionEvaluator} au point d'action.
 * C'est le cœur, et lui seul, qui sait qu'un compte figure dans les {@code admins} d'un serveur
 * et gagne donc {@code SERVER_START} sur celui-là ; le contrôle grossier du BFF ne peut pas
 * répondre à cette question (plan §A.2).</p>
 */
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

    /** Vue réduite pour un affichage non authentifié : aucun détail d'infrastructure. */
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
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.SERVER_CREATE, null);
        GameServer created = gameServerService.create(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(projectionService.toInfraDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameServerDto> update(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String id,
            @RequestBody GameServerDto dto) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.SERVER_EDIT, null);
        GameServer updated = gameServerService.update(id, mapper.toEntity(dto));
        return ResponseEntity.ok(projectionService.toInfraDto(updated));
    }

    /**
     * 202 : démarrer un déploiement est asynchrone. La boucle d'observation constatera le
     * passage à ONLINE et le poussera vers Discord — répondre 200 laisserait croire que c'est
     * déjà fait.
     *
     * <p>La permission est évaluée <em>sur ce serveur</em> : figurer dans ses {@code admins}
     * suffit, sans donner le moindre droit sur les autres (décision n°11 du 18-09).</p>
     */
    @PostMapping("/{slug}/start")
    public ResponseEntity<Void> start(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String slug) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId),
                Permission.SERVER_START, ResourceRef.gameServer(slug));
        deploymentService.start(gameServerService.requireBySlug(slug));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{slug}/stop")
    public ResponseEntity<Void> stop(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String slug) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId),
                Permission.SERVER_STOP, ResourceRef.gameServer(slug));
        deploymentService.stop(gameServerService.requireBySlug(slug));
        return ResponseEntity.accepted().build();
    }

    /** Les jeux connus, pour alimenter un menu déroulant sans les coder en dur côté interface. */
    @GetMapping("/games")
    public List<GameDto> games(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        readActor(actorDiscordId).ifPresent(user -> permissionEvaluator.require(user, Permission.SERVER_VIEW, null));
        return java.util.Arrays.stream(Game.values())
                .map(game -> new GameDto(game.name(), game.getLabel(), game.getIconUrl()))
                .toList();
    }

    /**
     * L'acteur d'une lecture, quand il y en a un.
     *
     * <p><strong>En-tête absent = service Schub agissant pour son compte</strong> — c'est le cas
     * du pull périodique du connecteur Discord, qui construit les cartes d'état d'un salon sans
     * personne derrière. Il reçoit la projection membre, jamais l'infra : un salon Discord n'est
     * pas un endroit où publier la liste des ports ouverts.</p>
     *
     * <p>En-tête <em>présent mais inconnu</em>, en revanche, est un refus — sinon un identifiant
     * inventé vaudrait mieux qu'un vrai.</p>
     */
    private Optional<User> readActor(String actorDiscordId) {
        if (actorDiscordId == null || actorDiscordId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(userService.requireActor(actorDiscordId));
    }

    public record GameDto(String name, String label, String iconUrl) {
    }
}
