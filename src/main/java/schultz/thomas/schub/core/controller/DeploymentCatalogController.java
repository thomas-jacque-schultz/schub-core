package schultz.thomas.schub.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.model.PortainerStack;
import schultz.thomas.schub.core.service.ContainerRequestService;

import java.util.List;

/**
 * Le catalogue des déploiements disponibles, pour lier un serveur sans saisie manuelle.
 *
 * <p>{@code deploymentId} est une clé de liaison : une faute de frappe ne se voit qu'au premier
 * démarrage raté. Proposer la liste supprime la classe d'erreur entière.</p>
 *
 * <p>Le connecteur rapporte <em>tous</em> les déploiements, y compris ceux d'infrastructure :
 * c'est ici, et au-dessus, qu'on trie.</p>
 */
@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentCatalogController {

    @Qualifier("portainerRequestService")
    private final ContainerRequestService containerRequestService;

    @GetMapping
    public List<PortainerStack> all() {
        return containerRequestService.listStacks();
    }
}
