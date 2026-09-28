package schultz.thomas.schub.core.team.business.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// La maintenance des données du connecteur Riot : les jeter après un changement de clé, file d'ingest à l'arrêt.
public interface RiotDataGateway {

    Optional<IngestPause> pause();

    IngestPause setPause(boolean paused);

    Optional<Inventory> inventory();

    // IllegalStateException si l'ingest n'est pas à l'arrêt ; RestClientException si le connecteur ne répond pas.
    Map<String, Long> purge();

    // running : tâches prises avant la pause et pas encore terminées.
    record IngestPause(boolean paused, Instant updatedAt, long running) {
    }

    record Inventory(Map<String, Long> purged, List<String> kept) {
    }
}
