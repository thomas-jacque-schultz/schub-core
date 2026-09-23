package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

public record UserDto(
        String id,
        String discordId,
        String discordUsername,
        String displayName,
        String avatarUrl,
        String roleId,
        String roleName,
        String riotGameName,
        String riotTagLine,
        Instant createdAt,
        Instant lastLoginAt
) {
}
