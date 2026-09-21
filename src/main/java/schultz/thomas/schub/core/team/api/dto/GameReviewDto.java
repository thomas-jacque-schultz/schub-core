package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

/**
 * Une note de débrief, servie au fil.
 *
 * @param authorMemberId  la place de l'auteur dans cette équipe, ou {@code null} s'il n'en est pas
 *                        membre — un {@code OWNER} peut écrire sans y avoir de place. Jamais un
 *                        identifiant de compte
 * @param viewerCanEdit   « ai-je le droit de modifier cette note ? » — un fait sur le lecteur, et
 *                        non la désignation de l'ayant droit à comparer côté client (plan §A.5 bis)
 */
public record GameReviewDto(
        String id,
        String matchId,
        String subjectMemberId,
        String subjectDisplayName,
        String authorMemberId,
        String authorDisplayName,
        String content,
        Instant createdAt,
        Instant updatedAt,
        boolean viewerCanEdit
) {
}
