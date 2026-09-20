package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Une équipe : un nom, un créateur, un effectif.
 *
 * <p>La collection est préfixée {@code team_} (plan §D.2) : elle vit dans la base {@code servers}
 * du cœur, qui a déjà son utilisateur Mongo — il n'y a rien à créer côté infrastructure, et c'est
 * une des raisons pour lesquelles ce domaine n'est pas un service. Le préfixe est ce qui rendra
 * l'extraction lisible : {@code mongodump} sur {@code team_*} et rien d'autre.</p>
 *
 * <p><strong>L'effectif est embarqué, pas référencé.</strong> Un membre n'a aucune existence hors
 * de son équipe — il n'est pas une personne, c'est la place d'une personne dans une équipe. Une
 * collection séparée n'aurait servi qu'à faire deux lectures pour afficher une page.</p>
 */
@Data
@Document(collection = "team_teams")
public class Team {

    @Id
    private String id;

    private String name;

    /**
     * L'id interne du compte qui a créé l'équipe — le capitaine.
     *
     * <p>C'est lui, et {@code OWNER}, qui peuvent la modifier (plan §D.2 bis, point 2). Un id
     * interne et non un identifiant Discord : le jour où un compte se lie autrement, un
     * identifiant externe rendrait le champ faux (plan §A.4).</p>
     */
    private String createdBy;

    private List<TeamMember> members = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public Optional<TeamMember> findMember(String memberId) {
        return members == null || memberId == null
                ? Optional.empty()
                : members.stream().filter(member -> memberId.equals(member.getMemberId())).findFirst();
    }

    /** Ce compte fait-il partie de l'effectif ? La comparaison porte sur l'id interne. */
    public boolean hasMemberLinkedTo(String userId) {
        return userId != null && members != null
                && members.stream().anyMatch(member -> userId.equals(member.getUserId()));
    }
}
