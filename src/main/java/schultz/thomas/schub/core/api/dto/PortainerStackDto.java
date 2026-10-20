package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record PortainerStackDto(
        Integer id,
        String name,
        @Nullable Integer endpointId,
        boolean running,
        Instant observedAt,
        GameServerCommandFailureDto lastCommandFailure
) {
}
