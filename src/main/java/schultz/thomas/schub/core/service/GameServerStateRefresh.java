package schultz.thomas.schub.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.model.GameServer;

/**
 * La boucle de réconciliation : observe chaque serveur, enregistre, notifie, puis réaligne
 * les redirections de ports.
 *
 * <p>C'est elle qui rend le système correct sans garantie de livraison. Un ordre perdu, un
 * redémarrage à contretemps, un serveur mort sans nettoyer : tout est rattrapé au passage
 * suivant. C'est le raisonnement qui a permis d'abandonner le broker (plan §5).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerStateRefresh {

    private final GameServerService gameServerService;
    private final DeploymentService deploymentService;
    private final PortForwardingService portForwardingService;
    private final DiscordNotifier discordNotifier;

    public void refresh() {
        try {
            for (GameServer gameServer : gameServerService.findAll()) {
                boolean changed = deploymentService.observe(gameServer);
                // Enregistré à chaque passage, pas seulement au changement : sinon
                // lastStatusCheckAt vaudrait en base la date du dernier *changement*, et
                // mentirait sur la fraîcheur de l'observation après un redémarrage.
                gameServerService.persistObservedState(gameServer);
                if (changed) {
                    log.info("{} est passé à {}", gameServer.getSlug(), gameServer.getStatus());
                    discordNotifier.gameServerChanged(gameServer.getSlug());
                }
            }
            portForwardingService.reconcile();
        } catch (Exception e) {
            // La boucle ne doit jamais mourir : une exception non rattrapée ici arrêterait
            // définitivement la réconciliation, et le système perdrait sa seule autocorrection.
            log.error("Échec d'un passage de la boucle d'observation", e);
        }
    }
}
