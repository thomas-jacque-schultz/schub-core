package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record UserDto(
        String id,
        @Nullable String discordId,
        @Nullable String discordUsername,
        @Nullable String displayName,
        @Nullable String avatarUrl,
        @Nullable String roleId,
        @Nullable String roleName,
        @Nullable String riotGameName,
        @Nullable String riotTagLine,
        @Nullable Instant createdAt,
        @Nullable Instant lastLoginAt,
        AccountLinksDto links
) {
}
