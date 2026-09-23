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

    @RollbackExecution
    public void rollback() {
        log.warn("Retour en arrière sans objet : un poste unique ne peut pas représenter plusieurs postes");
    }
}
