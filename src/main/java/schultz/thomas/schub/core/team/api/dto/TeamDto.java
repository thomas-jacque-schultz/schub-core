package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Une équipe ouverte : son effectif, et ce que <em>le lecteur</em> a le droit d'y faire.
 *
 * <h2>Les trois booléens, et pourquoi ils ont cette forme</h2>
 *
 * <p>Quand un écran doit savoir « ai-je le droit sur cet objet ? », la réponse est un
 * <strong>fait sur le lecteur</strong>, jamais la liste des ayants droit — la liste est une
 * information sur les autres, et l'exposer est une fuite (plan §A.5 bis). C'est exactement la
 * forme de {@code viewerIsAdmin} sur {@code GameServerDto}, et pour la même raison : sans elle,
 * le front propose les boutons à tout le monde et le cœur refuse après le clic.</p>
 *
 * <p>Concrètement, cette projection ne dit jamais <em>qui</em> peut modifier l'équipe. Elle dit
 * si <em>vous</em> le pouvez. Un {@code OWNER} le voit vrai par son rôle, le capitaine par sa
 * place, et ni l'un ni l'autre n'apprend quoi que ce soit sur le troisième.</p>
 *
 * @param viewerMemberId          la place du lecteur dans cette équipe, ou {@code null} s'il n'en
 *                                est pas membre. Un fait sur lui : c'est ce qui permet à un écran
 *                                de se surligner sans comparer des identifiants
 * @param viewerCanEdit           peut-il renommer l'équipe et toucher à l'effectif ?
 * @param viewerCanEditCompositions peut-il écrire les compositions ? Distinct du précédent parce
 *                                que les deux permissions sont distinctes, même si elles vont
 *                                aujourd'hui aux mêmes personnes
 */
public record TeamDto(
        String id,
        String name,
        int memberCount,
        List<TeamMemberDto> members,
        Instant createdAt,
        Instant updatedAt,
        String viewerMemberId,
        boolean viewerCanEdit,
        boolean viewerCanEditCompositions
) {
}
