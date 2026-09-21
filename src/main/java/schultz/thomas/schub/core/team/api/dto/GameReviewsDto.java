package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Les notes d'une partie d'équipe.
 *
 * @param viewerCanReviewAnyone le lecteur peut-il écrire sur n'importe quelle place ? Faux, il ne
 *                              peut écrire que sur {@link #viewerMemberId} — et sur personne si
 *                              celui-ci est {@code null}. Deux faits sur lui, aucun sur les autres
 */
public record GameReviewsDto(
        String teamId,
        String matchId,
        List<GameReviewDto> reviews,
        String viewerMemberId,
        boolean viewerCanReviewAnyone,
        Instant generatedAt
) {
}
