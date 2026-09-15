package schultz.thomas.schub.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Pousse vers le connecteur Discord quand un serveur change.
 *
 * <p>« Push pour la latence, pull pour la correction » (plan §5). Ce push fait bouger le message
 * Discord tout de suite ; s'il se perd, le pull périodique du connecteur corrigera. C'est
 * exactement pourquoi aucune garantie de livraison — donc aucun broker — n'est nécessaire.</p>
 *
 * <p>D'où l'absence de gestion d'erreur au-delà d'un log : un échec ici est sans conséquence, et
 * le faire remonter ferait échouer une écriture pourtant réussie.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordNotifier {

    @Qualifier("connectorDiscordRestClient")
    private final RestClient restClient;

    @Async
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
