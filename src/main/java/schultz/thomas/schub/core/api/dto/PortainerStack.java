package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

public record PortainerStack(
        Integer id,
        String name,
        Integer endpointId,
        boolean running,
        Instant observedAt
) {
}
