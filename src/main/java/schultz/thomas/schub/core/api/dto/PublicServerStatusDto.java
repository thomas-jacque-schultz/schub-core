package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;

public record PublicServerStatusDto(
        @Nullable String name,
        @Nullable String game,
        @Nullable String status
) {
}
