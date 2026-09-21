package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * <strong>Le panneau 3 d'une page d'équipe</strong> : ce que l'équipe peut aligner à chaque poste.
 *
 * <h2>La question a changé</h2>
 *
 * <p>Le panneau répondait « voici ce que chaque membre maîtrise ». Il répond maintenant « voici ce
 * qu'on peut aligner à ce poste » : on choisit des champions dans le catalogue, et le panneau dit
 * qui de l'effectif tient le poste et les maîtrise assez. La première question est une
 * conséquence de la seconde, pas l'inverse.</p>
 *
 * <h2>{@code patch} n'est pas décoratif</h2>
 *
 * <p>Un nom et une icône dépendent de la version de Data Dragon. Sans elle, une donnée relue dans
 * trois mois est fausse sans que rien ne le signale. La règle est donc absolue :
 * <strong>si {@code patch} est nul, ni catalogue ni champion n'est rendu</strong>, et le choix
 * enregistré n'est pas perdu pour autant — il est seulement inaffichable.</p>
 *
 * @param masteryFloor     le plancher <strong>appliqué</strong> à cette réponse
 * @param teamMasteryFloor celui qu'a enregistré l'équipe. Les deux diffèrent quand l'appelant a
 *                         demandé un plancher pour voir ; la lecture n'écrit jamais, et l'écran
 *                         peut dire lequel il montre
 * @param catalog          tous les champions du patch, pour qu'on puisse en choisir. Servi avec
 *                         le panneau et non par une route à part : deux appels, ce sont deux
 *                         patches possibles
 * @param viewerCanEdit    peut-il changer le choix et le plancher ? Un fait sur le lecteur, pas
 *                         la liste des ayants droit (plan §A.5 bis)
 */
public record ChampionPoolDto(
        String teamId,
        String teamName,
        String patch,
        int masteryFloor,
        int teamMasteryFloor,
        List<ChampionCatalogEntryDto> catalog,
        List<ChampionPoolColumnDto> columns,
        String viewerMemberId,
        boolean viewerCanEdit,
        Instant generatedAt
) {
}
