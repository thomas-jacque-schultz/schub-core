package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import schultz.thomas.schub.core.team.business.model.GameRole;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ce que l'équipe a décidé de pouvoir aligner, poste par poste.
 *
 * <p>C'est le choix qui manquait au panneau : les maîtrises disent ce que <em>chacun</em> sait
 * jouer, pas ce que <em>l'équipe</em> est prête à poser sur la Faille. Le second est un jugement,
 * il se prend une fois et il doit survivre à la fermeture de l'onglet — d'où un document.</p>
 *
 * <p>L'identifiant <strong>est</strong> celui de l'équipe : une équipe a un pool et un seul, et
 * la contrainte est portée par la clé plutôt que par un index à tenir.</p>
 *
 * <p>Les champions sont désignés par leur <strong>clé Data Dragon</strong> ({@code MonkeyKing}),
 * comme dans une composition. L'identifiant numérique aurait aussi marché ; la clé est ce que le
 * reste du domaine manipule déjà, et mélanger les deux est la façon la plus sûre de ne plus
 * retrouver un champion.</p>
 *
 * <p><strong>La clé de la map est une chaîne, et la conversion se fait ici.</strong> Un
 * {@code Map<GameRole, …>} se serait relu en clés de chaîne si le convertisseur ne les avait pas
 * converties, et un {@code get(GameRole.TOP)} aurait alors répondu « aucun champion » sans lever
 * la moindre erreur — une colonne vide, et rien à quoi le rattacher.</p>
 */
@Data
@Document(collection = "team_champion_pools")
public class TeamChampionPool {

    @Id
    private String teamId;

    private Map<String, List<String>> championKeysByRole = new LinkedHashMap<>();

    /**
     * Le nombre de points de maîtrise en dessous duquel un joueur n'est pas proposé sur un
     * champion.
     *
     * <p><strong>Il appartient à l'équipe</strong>, il n'est pas un réglage d'affichage. Le
     * plancher change la réponse à « qui peut jouer ça ? » ; s'il vivait dans l'écran de chacun,
     * deux membres regardant le même poste liraient deux listes différentes et se croiraient
     * d'accord. Un réglage à la volée reste possible en lecture, et il n'écrit rien.</p>
     */
    private int masteryFloor;

    private Instant updatedAt;

    public List<String> championKeys(GameRole role) {
        if (championKeysByRole == null || role == null) {
            return List.of();
        }
        return championKeysByRole.getOrDefault(role.name(), List.of());
    }

    public void setChampionKeys(GameRole role, List<String> keys) {
        if (championKeysByRole == null) {
            championKeysByRole = new LinkedHashMap<>();
        }
        championKeysByRole.put(role.name(), new ArrayList<>(keys));
    }
}
