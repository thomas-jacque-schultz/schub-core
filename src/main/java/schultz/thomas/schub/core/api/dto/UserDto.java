package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

/**
 * Un compte tel que le cœur l'expose.
 *
 * <p>{@code roleName} accompagne {@code roleId} pour que l'écran des utilisateurs affiche le
 * rôle sans un second appel par ligne. Les champs Riot sont là pour le chantier D ; ils restent
 * nuls jusqu'à la liaison de compte (lot D.3).</p>
 */
public record UserDto(
        String id,
        String discordId,
        String discordUsername,
        String avatarUrl,
        String roleId,
        String roleName,
        String riotGameName,
        String riotTagLine,
        Instant createdAt,
        Instant lastLoginAt
) {
}
