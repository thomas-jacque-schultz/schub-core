package schultz.thomas.schub.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.dto.GameServerDto;
import schultz.thomas.schub.core.dto.PublicServerStatusDto;
import schultz.thomas.schub.core.mapper.GameServerMapper;
import schultz.thomas.schub.core.model.Game;
import schultz.thomas.schub.core.model.GameServer;
import schultz.thomas.schub.core.service.DeploymentService;
import schultz.thomas.schub.core.service.GameServerService;

import java.util.List;

/**
 * L'API du domaine. Tout ce qui concerne un serveur passe par ici — le BFF, le connecteur
 * Discord qui tire périodiquement, et rien d'autre.
 */
@RestController
@RequestMapping("/game-servers")
@RequiredArgsConstructor
public class GameServerController {

    private final GameServerService gameServerService;
    private final DeploymentService deploymentService;
    private final GameServerMapper mapper;

    @GetMapping
    public List<GameServerDto> all() {
        return gameServerService.findAll().stream().map(mapper::toDto).toList();
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
    public ResponseEntity<GameServerDto> byId(@PathVariable String id) {
        return gameServerService.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GameServerDto> create(@RequestBody GameServerDto dto) {
        GameServer created = gameServerService.create(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameServerDto> update(@PathVariable String id, @RequestBody GameServerDto dto) {
        GameServer updated = gameServerService.update(id, mapper.toEntity(dto));
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    /**
     * 202 : démarrer un déploiement est asynchrone. La boucle d'observation constatera le
     * passage à ONLINE et le poussera vers Discord — répondre 200 laisserait croire que c'est
     * déjà fait.
     */
    @PostMapping("/{slug}/start")
    public ResponseEntity<Void> start(@PathVariable String slug) {
        deploymentService.start(gameServerService.requireBySlug(slug));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{slug}/stop")
    public ResponseEntity<Void> stop(@PathVariable String slug) {
        deploymentService.stop(gameServerService.requireBySlug(slug));
        return ResponseEntity.accepted().build();
    }

    /** Les jeux connus, pour alimenter un menu déroulant sans les coder en dur côté interface. */
    @GetMapping("/games")
    public List<GameDto> games() {
        return java.util.Arrays.stream(Game.values())
                .map(game -> new GameDto(game.name(), game.getLabel(), game.getIconUrl()))
                .toList();
    }

    public record GameDto(String name, String label, String iconUrl) {
    }
}
