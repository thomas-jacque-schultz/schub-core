package schultz.thomas.schub.core.data.model;

import java.time.Instant;

public record GameServerCommandFailure(
        String action,
        String message,
        Instant at
) {
}
