package schultz.thomas.schub.core.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record GameServerCommandFailureDto(
        @Schema(allowableValues = {"start", "stop"}) String action,
        @Nullable String message,
        @Nullable Instant at
) {
}
