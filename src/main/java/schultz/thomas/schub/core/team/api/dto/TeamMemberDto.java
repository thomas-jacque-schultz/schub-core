package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.util.List;

/**
 * Un membre tel qu'il est servi sur le fil.
 *
 * <p><strong>Ce qui n'y figure pas est aussi important que ce qui y figure : l'{@code userId}
 * des autres.</strong> Le front n'en a aucun usage — il n'a jamais à comparer des identifiants
 * pour savoir ce qu'il a le droit de faire, puisque {@link TeamDto} le lui dit directement
 * (plan §A.5 bis). Le servir serait distribuer les identifiants internes des comptes d'autrui
 * pour rien.</p>
 *
 * @param memberId    l'identifiant de cette place dans cette équipe — c'est lui qu'on renvoie
 *                    pour modifier, retirer ou désigner ce membre dans une composition
 * @param displayName de quoi le reconnaître : son pseudo Discord s'il est lié, son Riot ID sinon
 * @param linked      a-t-il un compte Schub ? Un membre libre n'a que son Riot ID, et c'est
 *                    exactement ce qui permet de constituer une équipe avant que les cinq se
 *                    soient connectés
 * @param roles       les postes tenus, du plus habituel au moins habituel. Vide pour un coach,
 *                    et pour un membre dont personne n'a encore dit à quel poste il joue
 * @param captain     ce membre est-il le créateur de l'équipe ? C'est un fait sur l'équipe, pas
 *                    la liste de ceux qui peuvent l'écrire
 */
public record TeamMemberDto(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        List<GameRole> roles,
        MemberStatus status,
        boolean linked,
        boolean captain
) {
}
