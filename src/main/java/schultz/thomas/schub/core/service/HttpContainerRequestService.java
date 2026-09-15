package schultz.thomas.schub.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import schultz.thomas.schub.core.config.ConnectorPortainerProperties;
import schultz.thomas.schub.core.model.DockerContainerState;
import schultz.thomas.schub.core.model.PortainerStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Implémentation de {@link ContainerRequestService} déléguant à {@code schub-connector-portainer}.
 *
 * <p>Depuis la phase 2, ce service ne sonde plus Portainer : le connecteur le fait une fois par
 * minute pour tout le monde, et sert un cache. Les lectures d'ici sont donc gratuites, quel que
 * soit le nombre de serveurs.</p>
 */
@Slf4j
@Service("portainerRequestService")
@RequiredArgsConstructor
public class HttpContainerRequestService implements ContainerRequestService {

    @Qualifier("connectorPortainerRestClient")
    private final RestClient restClient;

    private final ConnectorPortainerProperties properties;

    @Override
    public boolean startContainer(Integer stackId) {
        restClient.post()
                .uri("/stacks/{id}/start", stackId)
                .retrieve()
                .toBodilessEntity();
        return true;
    }

    @Override
    public boolean stopContainer(Integer stackId) {
        restClient.post()
                .uri("/stacks/{id}/stop", stackId)
                .retrieve()
                .toBodilessEntity();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Lève plutôt que de rendre un état lorsque la lecture est trop ancienne. L'appelant
     * traduit toute exception en {@code UNREACHABLE}, ce qui est exactement le sens voulu :
     * une valeur périmée est une absence de réponse, pas la preuve que le serveur est éteint
     * (plan §6). Rendre {@code running=false} sur un cache figé ferait croire à une extinction
     * et déclencherait la fermeture des redirections d'un serveur pourtant en marche.</p>
     */
    @Override
    public DockerContainerState getContainerState(Integer stackId) {
        PortainerStack stack = restClient.get()
                .uri("/stacks/{id}", stackId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PortainerStack.class);

        if (stack == null) {
            throw new IllegalStateException("Stack " + stackId + " inconnue du connecteur Portainer");
        }
        requireFresh(stack);

        DockerContainerState state = new DockerContainerState();
        state.setRunning(stack.running());
        return state;
    }

    @Override
    public List<PortainerStack> listStacks() {
        List<PortainerStack> stacks = restClient.get()
                .uri("/stacks")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return stacks == null ? List.of() : stacks;
    }

    private void requireFresh(PortainerStack stack) {
        if (stack.observedAt() == null) {
            throw new IllegalStateException("Le connecteur Portainer n'a pas daté sa réponse");
        }
        Duration age = Duration.between(stack.observedAt(), Instant.now());
        if (age.compareTo(properties.getStaleAfter()) > 0) {
            log.warn("État de la stack {} trop ancien ({}s) : traité comme une absence de réponse",
                    stack.id(), age.toSeconds());
            throw new IllegalStateException("État Portainer périmé de " + age.toSeconds() + "s");
        }
    }
}
