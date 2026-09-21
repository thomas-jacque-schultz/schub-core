package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;

import java.time.Instant;

/**
 * Un membre sous un champion : il tient ce poste, et voici ce qu'il vaut dessus.
 *
 * <p>Comme {@code TeamMemberDto}, il ne porte <strong>pas</strong> l'{@code userId} des autres
 * (plan §A.5 bis).</p>
 *
 * @param state         pourquoi la maîtrise vaut ce qu'elle vaut. Un membre dont on ne sait rien
 *                      n'est pas retiré du panneau : il est rendu au niveau de la colonne, avec
 *                      la raison
 * @param masteryPoints {@code 0} est une réponse — il tient ce poste et n'a jamais touché ce
 *                      champion. {@code null} veut dire qu'on n'a pas su lire ses maîtrises
 */
public record ChampionPoolMemberDto(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        MemberStatus status,
        boolean linked,
        PoolState state,
        Integer masteryLevel,
        Integer masteryPoints,
        Instant lastPlayedAt,
        Instant observedAt
) {
}
