package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

public record GameServerCommandFailureDto(
        String action,
        String message,
        Instant at
) {
}
