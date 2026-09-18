package schultz.thomas.schub.core.api.dto;

/**
 * Un administrateur de serveur, résolu pour l'affichage.
 *
 * <p>{@code GameServer.admins} contient des <strong>ids internes</strong> depuis le 18-09. Le
 * cœur résout le pseudo et l'avatar ici, une fois, plutôt que de laisser le front faire N appels
 * pour afficher une liste de cinq noms (plan §A.4).</p>
 */
public record ServerAdminDto(
        String userId,
        String discordUsername,
        String avatarUrl
) {
}
