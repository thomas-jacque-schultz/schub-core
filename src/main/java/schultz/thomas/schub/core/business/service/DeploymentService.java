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

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    @Qualifier("portainerRequestService")
    private final ContainerRequestService containerRequestService;

    private final PortForwardingService portForwardingService;

    // Exception = UNREACHABLE, pas OFFLINE : confondre fermerait les redirections d'un serveur en marche.
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

    // Ports ouverts avant le démarrage, pour que le jeu les trouve prêts. Un échec d'ouverture ne bloque pas.
    public boolean start(GameServer gameServer) {
        portForwardingService.reconcile(gameServer.getSlug(), true);
        return containerRequestService.startContainer(gameServer.getDeploymentId());
    }

    public boolean stop(GameServer gameServer) {
        boolean stopped = containerRequestService.stopContainer(gameServer.getDeploymentId());
        if (stopped) {
            portForwardingService.reconcile(gameServer.getSlug(), false);
        }
        return stopped;
    }
}
