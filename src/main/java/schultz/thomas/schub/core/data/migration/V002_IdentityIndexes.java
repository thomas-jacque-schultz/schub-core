package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;

@ChangeUnit(id = "identity-indexes", order = "002", author = "schub")
public class V002_IdentityIndexes {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class)
                .ensureIndex(new Index().on("discordId", Sort.Direction.ASC).unique().named("discordId_unique"));
        mongoTemplate.indexOps(Role.class)
                .ensureIndex(new Index().on("name", Sort.Direction.ASC).unique().named("role_name_unique"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).dropIndex("discordId_unique");
        mongoTemplate.indexOps(Role.class).dropIndex("role_name_unique");
    }
}
