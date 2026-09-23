package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.data.model.GameServer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
                gameServerService.persistObservedState(gameServer, changed);
                if (changed) {
                    log.info("{} est passé à {}", gameServer.getSlug(), gameServer.getStatus());
                    discordNotifier.gameServerChanged(gameServer.getSlug());
                }
            }
            portForwardingService.reconcile();
        } catch (Exception e) {
            log.error("Échec d'un passage de la boucle d'observation", e);
        }
    }
}
