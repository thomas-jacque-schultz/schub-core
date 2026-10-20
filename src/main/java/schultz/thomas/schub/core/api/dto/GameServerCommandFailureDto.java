package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record GameServerCommandFailureDto(
        String action,
        @Nullable String message,
        @Nullable Instant at
) {
}
