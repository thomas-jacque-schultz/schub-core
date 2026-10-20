package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;

public record GameServerReferentDto(
        String id,
        @Nullable String displayName,
        @Nullable String discordId
) {
}
