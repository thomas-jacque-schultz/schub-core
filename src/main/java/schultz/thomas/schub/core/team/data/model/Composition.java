package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Une composition préparée : cinq postes, cinq champions, et les joueurs qu'on y met.
 *
 * <p><strong>Ce n'est pas une draft jouée, et surtout pas un simulateur</strong> (plan §D, lot
 * D.6) : pas de bans, pas d'ordre de pick, pas de minuteur. C'est un outil de proposition — on
 * prépare, on enregistre, on montre à l'équipe. Le simulateur complet reste possible plus tard
 * en réutilisant ce modèle, une composition étant l'état final d'une draft ; construire l'un en
 * croyant faire l'autre est ce qu'il fallait éviter.</p>
 *
 * <p><strong>Exactement cinq lignes, un poste chacune.</strong> C'est la seule contrainte de
 * cardinalité du domaine, et elle ne remonte pas à l'équipe : une équipe n'est pas limitée à
 * cinq, une composition en désigne cinq. Les deux règles sont séparées parce que ce sont deux
 * objets différents.</p>
 */
@Data
@Document(collection = "team_compositions")
public class Composition {

    @Id
    private String id;

    @Indexed
    private String teamId;

    private String name;

    private List<CompositionSlot> slots = new ArrayList<>();

    /**
     * La version Data Dragon au moment de la préparation.
     *
     * <p>Figée ici, sinon une composition relue dans trois mois afficherait des champions qui
     * n'existaient pas quand elle a été écrite.</p>
     */
    private String patch;

    private String notes;

    /** L'id interne du compte qui l'a écrite. Sert à l'afficher, pas à autoriser. */
    private String createdBy;

    private Instant createdAt;
    private Instant updatedAt;
}
