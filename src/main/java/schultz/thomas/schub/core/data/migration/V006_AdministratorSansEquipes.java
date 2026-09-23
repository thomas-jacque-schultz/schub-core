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
import schultz.thomas.schub.core.business.model.SystemRole;

import java.util.List;

@ChangeUnit(id = "administrator-sans-droits-equipe", order = "006", author = "schub")
public class V006_AdministratorSansEquipes {

    private static final Logger log = LoggerFactory.getLogger(V006_AdministratorSansEquipes.class);

    private static final List<String> PORTEES_PAR_EQUIPE = List.of(
            Permission.TEAM_VIEW.name(),
            Permission.TEAM_EDIT.name(),
            Permission.COMPOSITION_EDIT.name());

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        long modifies = mongoTemplate.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.ADMINISTRATOR.roleName()),
                Updates.pullAll("permissions", PORTEES_PAR_EQUIPE)).getModifiedCount();

        if (modifies == 0) {
            log.info("ADMINISTRATOR ne portait aucun droit d'équipe global — rien à retirer.");
        } else {
            log.info("ADMINISTRATOR : {} retirée(s). Les droits sur une équipe s'obtiennent en en "
                    + "étant capitaine ou membre.", PORTEES_PAR_EQUIPE);
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.ADMINISTRATOR.roleName()),
                Updates.addEachToSet("permissions", PORTEES_PAR_EQUIPE));
    }
}
