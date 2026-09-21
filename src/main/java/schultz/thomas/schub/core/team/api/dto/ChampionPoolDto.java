package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * <strong>Le panneau 3 d'une page d'équipe</strong> : cinq colonnes, une par poste, et pour
 * chaque membre les champions qu'il peut jouer d'après ses maîtrises.
 *
 * <h2>{@code patch} n'est pas décoratif</h2>
 *
 * <p>Un nom et une icône de champion dépendent de la version de Data Dragon. Sans elle, une
 * donnée relue dans trois mois est fausse sans que rien ne le signale — un champion rééquilibré,
 * renommé, ou qui n'existait pas. La version est donc figée dans la réponse, et la règle qui va
 * avec est absolue : <strong>si {@code patch} est nul, aucun champion n'est rendu nulle part</strong>.
 * Servir des icônes sans savoir de quel patch elles viennent serait pire que de ne rien servir.</p>
 *
 * <h2>Pourquoi aucun booléen {@code viewerCan…}</h2>
 *
 * <p>Le §A.5 bis du plan veut qu'un écran sache « ai-je le droit sur cet objet ? » par un fait sur
 * le lecteur. Ici, il n'y a aucun droit à arbitrer : ce panneau est en lecture seule, et son accès
 * est déjà tranché par {@code TEAM_VIEW} sur cette équipe — on ne reçoit pas la réponse si on n'y
 * a pas droit. Le seul fait sur le lecteur qui serve à quelque chose est
 * {@link #viewerMemberId} : c'est ce qui permet de surligner sa propre colonne sans comparer
 * d'identifiants. Ajouter des booléens que la page tient déjà de {@code TeamDto} coûterait une
 * seconde évaluation des permissions pour rien.</p>
 *
 * @param columns            toujours les cinq postes, dans l'ordre du jeu, y compris ceux que
 *                           personne ne tient — une colonne vide se dessine, une colonne absente
 *                           décale la page
 * @param membersWithoutRole les membres jouables à qui aucun poste n'est attribué. Ils ne sont
 *                           dans aucune colonne et ne doivent pas disparaître pour autant
 * @param championsPerMember le nombre de champions demandé par membre — le « top N » appliqué,
 *                           rendu pour qu'un écran puisse dire « les 10 plus maîtrisés » sans
 *                           le deviner
 * @param generatedAt        quand cette réponse a été assemblée. Ce n'est pas l'âge de la donnée
 *                           Riot : celui-là est porté membre par membre, par {@code observedAt}
 */
public record ChampionPoolDto(
        String teamId,
        String teamName,
        String patch,
        int championsPerMember,
        List<ChampionPoolColumnDto> columns,
        List<ChampionPoolMemberDto> membersWithoutRole,
        String viewerMemberId,
        Instant generatedAt
) {
}
