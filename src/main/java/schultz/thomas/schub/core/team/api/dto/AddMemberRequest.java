package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.util.List;

/**
 * Ajouter quelqu'un à l'effectif.
 *
 * <p><strong>On ajoute un Riot ID, pas un compte Schub</strong> (plan §D.2 bis, point 1) : exiger
 * un compte voudrait dire qu'aucune équipe n'existe tant que les cinq ne se sont pas connectés.
 * Si ce Riot ID appartient déjà à un compte, le lien est fait sur-le-champ ; sinon la place
 * l'attend, et la personne la revendiquera en liant son compte Riot.</p>
 *
 * @param riotPuuid facultatif. Fourni, il évite un appel au connecteur Riot ; absent, il est
 *                  résolu au mieux depuis {@code gameName}/{@code tagLine}, et son absence
 *                  n'empêche pas l'ajout — le membre sera rattrapé à la revendication
 * @param roles     les postes tenus, éventuellement plusieurs. Vide pour un coach, et pour un
 *                  membre dont on ne sait pas encore à quel poste il joue
 * @param status    {@code TITULAIRE} par défaut
 */
public record AddMemberRequest(
        String riotGameName,
        String riotTagLine,
        String riotPuuid,
        List<GameRole> roles,
        MemberStatus status
) {
}
