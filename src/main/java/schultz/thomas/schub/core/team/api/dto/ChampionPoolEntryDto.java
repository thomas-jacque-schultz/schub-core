package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

/**
 * Un champion retenu à un poste, et qui de l'équipe peut le jouer.
 *
 * <p>Les trois identités d'un champion cohabitent volontairement : {@code championId} recoupe les
 * maîtrises et {@code match-v5}, {@code championKey} est ce que retiennent le pool et les
 * compositions, {@code name} est ce qu'on affiche — traduit, et changeant.</p>
 *
 * @param players         ceux qui tiennent ce poste <strong>et</strong> passent le plancher, du
 *                        plus maîtrisé au moins maîtrisé. Vide est une réponse utile : ce
 *                        champion, personne ne le joue assez
 * @param setAsideByFloor combien le plancher a écartés. Sans ce compte, une liste vide se lit
 *                        comme une panne plutôt que comme l'effet du réglage qu'on vient de
 *                        monter
 */
public record ChampionPoolEntryDto(
        int championId,
        String championKey,
        String name,
        String iconUrl,
        List<ChampionPoolMemberDto> players,
        int setAsideByFloor
) {
}
