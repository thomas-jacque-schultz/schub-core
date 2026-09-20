package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

/**
 * Changer le poste ou la place d'un membre.
 *
 * <p>Ni le Riot ID ni le compte lié ne se modifient par ici : le premier appartient à Riot, le
 * second se gagne par revendication. Les rendre modifiables permettrait de donner la place de
 * quelqu'un à quelqu'un d'autre depuis un écran d'édition d'équipe.</p>
 *
 * @param role {@code null} retire le poste — c'est ce qu'on fait d'un joueur qui passe coach
 */
public record UpdateMemberRequest(GameRole role, MemberStatus status) {
}
