package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.AssignRoleRequest;
import schultz.thomas.schub.core.api.dto.UserDto;
import schultz.thomas.schub.core.api.dto.UserIdentityDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Set;

/**
 * Les comptes. Ces routes vivaient dans le connecteur Discord jusqu'au 18-09 ; elles sont ici
 * parce que l'identité est du domaine (plan §1).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;

    @GetMapping
    public List<UserDto> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        User actor = userService.requireActor(actorDiscordId);
        permissionEvaluator.require(actor, Permission.USER_VIEW, null);
        return userService.toDtos(userService.findAll());
    }

    /**
     * Qui est ce compte Discord, et que peut-il faire ? — <strong>le point d'entrée de la
     * connexion</strong>, appelé par le BFF après l'échange OAuth et par le connecteur Discord
     * avant d'exécuter une commande.
     *
     * <p><strong>Cette route crée le compte s'il est inconnu</strong>, au rôle {@code VISITEUR} :
     * tout le monde peut se connecter sans inscription préalable. Un {@code GET} qui écrit est
     * inhabituel et c'est délibéré — l'opération est idempotente et c'est la seule forme qui
     * convienne au retour d'une redirection OAuth, qui est un {@code GET} de navigateur.</p>
     *
     * <p>Elle n'exige aucune permission : elle <em>établit</em> l'identité, elle ne peut donc pas
     * la présupposer. Le secret interne reste ce qui la protège.</p>
     *
     * <p>{@code discordUsername} et {@code avatarUrl} sont facultatifs et viennent de l'appelant,
     * seul à avoir parlé à Discord : le cœur ne connaît pas l'API Discord et ne doit pas
     * l'apprendre.</p>
     */
    @GetMapping("/by-discord/{discordId}")
    public UserIdentityDto byDiscordId(@PathVariable String discordId,
                                       @RequestParam(required = false) String discordUsername,
                                       @RequestParam(required = false) String avatarUrl) {
        return userService.toIdentityDto(userService.findOrCreateByDiscordId(discordId, discordUsername, avatarUrl));
    }

    /**
     * Les permissions <em>effectives</em> de ce compte, éventuellement sur une ressource précise.
     *
     * <p>Sert au connecteur Discord, qui a cessé de juger lui-même : il pose la question, le cœur
     * répond, et il n'existe plus qu'une seule règle. Avec {@code gameServer}, la réponse inclut
     * ce que l'acteur tient d'être administrateur de <em>ce</em> serveur — ce qu'un jeu de
     * permissions de rôle seul ne dirait pas.</p>
     */
    @GetMapping("/by-discord/{discordId}/permissions")
    public Set<Permission> effectivePermissions(@PathVariable String discordId,
                                                @RequestParam(required = false) String discordUsername,
                                                @RequestParam(required = false) String gameServer) {
        User user = userService.findOrCreateByDiscordId(discordId, discordUsername, null);
        return permissionEvaluator.effectivePermissions(user, ResourceRef.gameServer(gameServer));
    }

    /** Attribution d'un rôle. Les règles anti-élévation sont dans le service, pas ici. */
    @PutMapping("/{id}/role")
    public UserDto assignRole(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                              @PathVariable String id,
                              @RequestBody AssignRoleRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return userService.toDto(userService.assignRole(actor, id, request.roleId()));
    }
}
