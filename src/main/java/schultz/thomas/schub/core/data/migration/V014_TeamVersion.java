package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

// Une équipe sans version serait prise pour une nouvelle et réinsérée : clé en double à la première écriture.
@ChangeUnit(id = "team-version", order = "014", author = "schub")
public class V014_TeamVersion {

    private static final Logger log = LoggerFactory.getLogger(V014_TeamVersion.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        long versionnees = mongoTemplate.getCollection("team_teams")
                .updateMany(Filters.exists("version", false), Updates.set("version", 0L))
                .getModifiedCount();
        log.info("Version posée sur {} équipe(s)", versionnees);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("team_teams").updateMany(Filters.exists("version"), Updates.unset("version"));
    }
}
