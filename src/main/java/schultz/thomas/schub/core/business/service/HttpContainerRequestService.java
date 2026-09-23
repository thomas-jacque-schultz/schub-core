package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.api.dto.PortainerStack;
import schultz.thomas.schub.core.business.model.DockerContainerState;
import schultz.thomas.schub.core.config.ConnectorPortainerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
