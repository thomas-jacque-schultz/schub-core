package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

/**
 * Une ligne d'une composition.
 *
 * @param memberId          la place désignée dans l'équipe, ou {@code null} si le poste n'est pas
 *                          encore attribué
 * @param playerDisplayName le nom à afficher pour cette place, résolu au moment de la lecture —
 *                          il n'est pas stocké dans la composition, sinon il serait faux le jour
 *                          où le joueur change de pseudo
 */
public record CompositionSlotDto(
        GameRole role,
        String championId,
        String memberId,
        String playerDisplayName
) {
}
