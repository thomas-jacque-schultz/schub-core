package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code TeamMember.role} devient {@code TeamMember.roles} : un membre tient plusieurs postes.
 *
 * <p>Le poste est un champ d'un sous-document embarqué dans un tableau, donc il n'existe pas de
 * mise à jour ensembliste portable qui le lise et l'écrive en une passe : chaque équipe est relue,
 * son effectif réécrit, et le lot part en {@code bulkWrite}.</p>
 *
 * <p><strong>Rejouable</strong> : seul un membre qui porte encore {@code role} et pas
 * {@code roles} est converti, et une équipe sans membre à convertir n'est pas réécrite. Un second
 * passage ne compte rien et n'écrit rien.</p>
 */
@ChangeUnit(id = "team-member-roles", order = "010", author = "schub")
public class V010_TeamMemberRoles {

    private static final Logger log = LoggerFactory.getLogger(V010_TeamMemberRoles.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var teams = mongoTemplate.getCollection("team_teams");
        List<WriteModel<Document>> lot = new ArrayList<>();

        for (Document team : teams.find(Filters.exists("members.role"))) {
            List<Document> members = team.getList("members", Document.class);
            if (members != null && convertit(members)) {
                lot.add(new UpdateOneModel<>(
                        Filters.eq("_id", team.get("_id")),
                        Updates.set("members", members)));
            }
        }

        if (lot.isEmpty()) {
            log.info("Aucun effectif à convertir : les postes sont déjà des listes");
            return;
        }
        log.info("Postes convertis en listes dans {} équipe(s)", teams.bulkWrite(lot).getModifiedCount());
    }

    /**
     * Convertit un effectif en place, et dit s'il a changé.
     *
     * <p>Un membre qui porte déjà {@code roles} n'est pas retouché — son {@code role} résiduel est
     * seulement retiré, sans compter pour une modification. C'est ce qui rend la migration
     * rejouable.</p>
     */
    static boolean convertit(List<Document> members) {
        boolean converti = false;
        for (Document member : members) {
            if (member.containsKey("roles")) {
                member.remove("role");
                continue;
            }
            Object role = member.remove("role");
            member.put("roles", role == null ? List.of() : List.of(role));
            converti = true;
        }
        return converti;
    }

    /**
     * Rien à rétablir : revenir à un poste unique demanderait de choisir lequel garder, et ce
     * choix ferait disparaître une donnée que personne n'aurait demandé à perdre.
     */
    @RollbackExecution
    public void rollback() {
        log.warn("Retour en arrière sans objet : un poste unique ne peut pas représenter plusieurs postes");
    }
}
