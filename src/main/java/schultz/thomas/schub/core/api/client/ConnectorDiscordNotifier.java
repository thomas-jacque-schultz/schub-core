package schultz.thomas.schub.core.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import schultz.thomas.schub.core.business.service.DiscordNotifier;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorDiscordNotifier implements DiscordNotifier {

    @Qualifier("connectorDiscordRestClient")
    private final RestClient restClient;

    @Async
    @Override
    public void gameServerChanged(String slug) {
        try {
            restClient.post()
                    .uri("/notifications/gameserver-changed")
                    .body(Map.of("slug", slug))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.debug("Notification Discord non délivrée pour '{}' ({}) — le pull périodique corrigera",
                    slug, e.getMessage());
        }
    }
}
