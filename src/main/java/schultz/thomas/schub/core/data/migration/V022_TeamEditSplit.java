package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.business.model.Permission;

import java.util.List;

// TEAM_EDIT n'existe plus dans l'enum : un rôle qui la garde en base ne se relit plus.
@ChangeUnit(id = "team-edit-split", order = "022", author = "schub")
public class V022_TeamEditSplit {

    private static final Logger log = LoggerFactory.getLogger(V022_TeamEditSplit.class);

    private static final String ANCIENNE = "TEAM_EDIT";

    private static final List<String> NOUVELLES =
            List.of(Permission.TEAM_MANAGE.name(), Permission.ROSTER_EDIT.name());

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var roles = mongoTemplate.getCollection("roles");
        long remplaces = roles.updateMany(Filters.eq("permissions", ANCIENNE),
                Updates.addEachToSet("permissions", NOUVELLES)).getModifiedCount();
        roles.updateMany(Filters.eq("permissions", ANCIENNE), Updates.pull("permissions", ANCIENNE));
        log.info("{} rôle(s) : {} remplacée par {}", remplaces, ANCIENNE, NOUVELLES);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        var roles = mongoTemplate.getCollection("roles");
        roles.updateMany(Filters.eq("permissions", Permission.TEAM_MANAGE.name()),
                Updates.addToSet("permissions", ANCIENNE));
        roles.updateMany(Filters.in("permissions", NOUVELLES), Updates.pullAll("permissions", NOUVELLES));
    }
}
