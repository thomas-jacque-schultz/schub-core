package schultz.thomas.schub.core.business.model;

import schultz.thomas.schub.core.data.model.GameServerCommandFailure;

import java.time.Instant;

public record PortainerStack(
        Integer id,
        String name,
        Integer endpointId,
        boolean running,
        Instant observedAt,
        GameServerCommandFailure lastCommandFailure
) {
}
