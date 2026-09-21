package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.util.List;

/**
 * Changer les postes ou la place d'un membre.
 *
 * <p>Ni le Riot ID ni le compte lié ne se modifient par ici : le premier appartient à Riot, le
 * second se gagne par revendication. Les rendre modifiables permettrait de donner la place de
 * quelqu'un à quelqu'un d'autre depuis un écran d'édition d'équipe.</p>
 *
 * @param roles la liste complète des postes — elle remplace l'ancienne. Vide ou {@code null} les
 *              retire tous, ce qu'on fait d'un joueur qui passe coach
 */
public record UpdateMemberRequest(List<GameRole> roles, MemberStatus status) {
}
