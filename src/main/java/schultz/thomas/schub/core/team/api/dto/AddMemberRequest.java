package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

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
 * @param role      le poste, ou {@code null} : un coach n'en a pas, un remplaçant polyvalent non
 *                  plus
 * @param status    {@code TITULAIRE} par défaut
 */
public record AddMemberRequest(
        String riotGameName,
        String riotTagLine,
        String riotPuuid,
        GameRole role,
        MemberStatus status
) {
}
