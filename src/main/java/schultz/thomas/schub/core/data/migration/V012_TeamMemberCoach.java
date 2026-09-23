package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

@ChangeUnit(id = "team-member-coach", order = "012", author = "schub")
public class V012_TeamMemberCoach {

    private static final Logger log = LoggerFactory.getLogger(V012_TeamMemberCoach.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        long modifiees = mongoTemplate.getCollection("team_teams").updateMany(
                Filters.eq("members.status", "COACH"),
                Updates.set("members.$[coach].coach", true),
                new UpdateOptions().arrayFilters(List.of(Filters.eq("coach.status", "COACH"))))
                .getModifiedCount();
        log.info("Attribut coach posé sur les coachs existants de {} équipe(s)", modifiees);
    }

    @RollbackExecution
    public void rollback() {
        log.warn("Retour en arrière sans objet : l'attribut coach est ignoré par les versions précédentes");
    }
}
