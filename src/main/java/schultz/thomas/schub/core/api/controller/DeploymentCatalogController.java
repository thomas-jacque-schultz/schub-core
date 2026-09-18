package schultz.thomas.schub.core.api.controller;

import schultz.thomas.schub.core.api.dto.PortainerStack;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.ContainerRequestService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Le catalogue des déploiements disponibles, pour lier un serveur sans saisie manuelle.
 *
 * <p>{@code deploymentId} est une clé de liaison : une faute de frappe ne se voit qu'au premier
 * démarrage raté. Proposer la liste supprime la classe d'erreur entière.</p>
 *
 * <p>Le connecteur rapporte <em>tous</em> les déploiements, y compris ceux d'infrastructure :
 * c'est ici, et au-dessus, qu'on trie.</p>
 *
 * <p>Derrière {@code SERVER_INFRA_VIEW}, et pas {@code SERVER_CREATE} : cette liste <em>est</em>
 * l'inventaire des stacks de la machine, y compris celles qui n'ont rien à voir avec un jeu.
 * C'est de la cartographie d'infrastructure, quel que soit l'usage qu'on en fait ensuite.</p>
 */
@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentCatalogController {

    @Qualifier("portainerRequestService")
    private final ContainerRequestService containerRequestService;

    private final PermissionEvaluator permissionEvaluator;

    private final UserService userService;

    @GetMapping
    public List<PortainerStack> all(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.SERVER_INFRA_VIEW, null);
        return containerRequestService.listStacks();
    }
}
