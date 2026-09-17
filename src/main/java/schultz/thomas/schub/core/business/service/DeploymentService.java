package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.business.model.DockerContainerState;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.GameServerStatus;
import schultz.thomas.schub.core.data.model.GameServerStatusHistoryEntry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Le lien entre un GameServer et le déploiement qui le réalise.
 *
 * <p>Anciennement {@code DockerService}. Le cœur ne nomme plus l'outil : il pilote un
 * <em>déploiement</em>, et c'est le connecteur qui sait que cela s'appelle une stack Portainer.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    @Qualifier("portainerRequestService")
    private final ContainerRequestService containerRequestService;

    private final PortForwardingService portForwardingService;

    /**
     * Observe le déploiement et met l'entité à jour. Rend vrai si l'état a changé.
     *
     * <p>Une exception — déploiement injoignable, ou lecture trop ancienne pour être crue —
     * devient {@code UNREACHABLE} et non {@code OFFLINE} : ne pas savoir n'est pas la même
     * chose que savoir que c'est éteint, et confondre les deux fermerait les redirections
     * d'un serveur en marche.</p>
     *
     * <p>Un statut null au premier passage compte comme un changement, pour que l'état initial
     * soit notifié.</p>
     */
    public boolean observe(GameServer gameServer) {
        GameServerStatus newStatus;
        Instant now = Instant.now();
        try {
            DockerContainerState state = containerRequestService.getContainerState(gameServer.getDeploymentId());
            newStatus = resolveStatus(state);
        } catch (Exception e) {
            log.warn("Lecture du déploiement impossible pour '{}' [{}] : {}",
                    gameServer.getSlug(), e.getClass().getSimpleName(), e.getMessage());
            newStatus = GameServerStatus.UNREACHABLE;
        }

        gameServer.setLastStatusCheckAt(now);

        if (newStatus.equals(gameServer.getStatus())) {
            return false;
        }

        gameServer.setStatus(newStatus);
        gameServer.setLastStatusChangeAt(now);
        appendStatusHistory(gameServer, newStatus, now);
        return true;
    }

    private void appendStatusHistory(GameServer gameServer, GameServerStatus status, Instant startedAt) {
        List<GameServerStatusHistoryEntry> history = gameServer.getStatusHistory();
        if (history == null) {
            history = new ArrayList<>();
            gameServer.setStatusHistory(history);
        }
        history.add(new GameServerStatusHistoryEntry(status, startedAt));
    }

    private GameServerStatus resolveStatus(DockerContainerState state) {
        if (state == null) {
            return GameServerStatus.UNREACHABLE;
        }
        return state.isRunning() ? GameServerStatus.ONLINE : GameServerStatus.OFFLINE;
    }

    /**
     * Ouvre les redirections avant de lancer le déploiement : le jeu doit trouver son port déjà
     * ouvert quand il finit de démarrer. Un échec d'ouverture n'empêche pas le démarrage — le
     * serveur reste joignable en LAN et la réconciliation périodique rattrapera.
     */
    public boolean start(GameServer gameServer) {
        portForwardingService.reconcile(gameServer.getSlug(), true);
        return containerRequestService.startContainer(gameServer.getDeploymentId());
    }

    /** Referme les redirections après l'arrêt, et seulement si l'arrêt a réussi. */
    public boolean stop(GameServer gameServer) {
        boolean stopped = containerRequestService.stopContainer(gameServer.getDeploymentId());
        if (stopped) {
            portForwardingService.reconcile(gameServer.getSlug(), false);
        }
        return stopped;
    }
}
