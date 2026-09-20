package schultz.thomas.schub.core.team.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import schultz.thomas.schub.core.team.business.model.GameRole;

/**
 * Une ligne d'une composition : un poste, un champion, et le joueur qu'on y met.
 *
 * <p>{@code championId} est la clé Data Dragon ({@code Ahri}, {@code MonkeyKing}…), pas un nom
 * affiché : les noms sont traduits et changent, les clés non. Le <em>patch</em> qui donne son
 * sens à cette clé est porté par la composition, pas par la ligne — c'est ce qui permet de
 * rouvrir une composition de mars et de la voir juste (plan §D, « à anticiper »).</p>
 *
 * <p>{@code memberId} peut être nul, et c'est délibéré : une composition est un
 * <strong>brouillon</strong> qu'on prépare avant que l'effectif soit complet. Ce qui est exigé,
 * c'est cinq postes — pas cinq joueurs déjà nommés.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositionSlot {

    private GameRole role;

    private String championId;

    /** Renvoie à {@link TeamMember#getMemberId()} de la même équipe, ou {@code null}. */
    private String memberId;
}
