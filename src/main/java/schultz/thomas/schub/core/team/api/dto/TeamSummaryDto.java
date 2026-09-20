package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

/**
 * Une équipe dans une liste : de quoi dessiner une carte, sans son effectif.
 *
 * <p>Elle porte les mêmes faits sur le lecteur que {@link TeamDto} — une liste d'équipes est
 * précisément l'écran où il faut savoir, sans un appel par ligne, lesquelles on peut modifier.</p>
 */
public record TeamSummaryDto(
        String id,
        String name,
        int memberCount,
        Instant createdAt,
        Instant updatedAt,
        String viewerMemberId,
        boolean viewerCanEdit,
        boolean viewerCanEditCompositions
) {
}
