package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

/**
 * Une stack telle que le connecteur Portainer la rapporte.
 *
 * @param observedAt date de la lecture qui a produit cette valeur. Les réponses du connecteur
 *                   viennent d'un cache : sans cette date, démarrer une stack puis relire
 *                   aussitôt conclurait qu'elle est toujours éteinte.
 */
public record PortainerStack(
        Integer id,
        String name,
        Integer endpointId,
        boolean running,
        Instant observedAt
) {
}
