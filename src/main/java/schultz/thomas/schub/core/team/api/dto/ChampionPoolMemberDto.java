package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;

import java.time.Instant;
import java.util.List;

/**
 * Un membre dans le pool : qui il est, et ce qu'il sait jouer.
 *
 * <p>Comme {@code TeamMemberDto}, il ne porte <strong>pas</strong> l'{@code userId} des autres :
 * le front n'a jamais à comparer des identifiants pour savoir ce qu'il peut faire, et servir les
 * identifiants internes des comptes d'autrui serait une fuite sans contrepartie (plan §A.5 bis).</p>
 *
 * @param state      pourquoi {@link #champions} contient ce qu'il contient. <strong>Un membre
 *                   sans maîtrises est rendu quand même</strong>, avec la raison : ni échec de
 *                   la requête, ni disparition silencieuse
 * @param champions  vide dès que {@code state} n'est pas {@code MAITRISES_CONNUES}. Triés du plus
 *                   maîtrisé au moins maîtrisé, comme le connecteur les rend
 * @param observedAt la date du relevé des maîtrises chez Riot, {@code null} s'il n'y en a pas eu.
 *                   Le connecteur les garde six heures (plan §D.2 ter) : c'est ce champ qui dit
 *                   l'âge réel de ce qu'on affiche, et non l'instant de la requête
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
        List<ChampionPoolEntryDto> champions,
        Instant observedAt
) {
}
