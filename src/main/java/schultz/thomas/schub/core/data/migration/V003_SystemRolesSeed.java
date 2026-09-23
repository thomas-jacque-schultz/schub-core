package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;

import java.util.Arrays;
import java.util.List;

/**
 * Rien de discordbot.users : l'utilisateur Mongo servers n'a pas le droit de lire discordbot.
 * OWNER n'est pas semé ici mais par OwnerSeeder : il dépend d'une variable d'environnement.
 */
@ChangeUnit(id = "system-roles-seed", order = "003", author = "schub")
public class V003_SystemRolesSeed {

    private static final Logger log = LoggerFactory.getLogger(V003_SystemRolesSeed.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var roles = mongoTemplate.getCollection("roles");
        for (SystemRole role : SystemRole.values()) {
            if (roles.find(Filters.eq("name", role.roleName())).first() != null) {
                log.info("Rôle système {} déjà présent", role.roleName());
                continue;
            }
            roles.insertOne(new Document()
                    .append("name", role.roleName())
                    .append("permissions", role.permissions().stream().map(Permission::name).toList())
                    .append("system", true)
                    .append("_class", "schultz.thomas.schub.core.data.model.Role"));
            log.info("Rôle système {} créé avec {} permission(s)", role.roleName(), role.permissions().size());
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        List<String> names = Arrays.stream(SystemRole.values()).map(SystemRole::roleName).toList();
        mongoTemplate.getCollection("roles").deleteMany(Filters.and(
                Filters.in("name", names),
                Filters.eq("system", true)));
    }
}
