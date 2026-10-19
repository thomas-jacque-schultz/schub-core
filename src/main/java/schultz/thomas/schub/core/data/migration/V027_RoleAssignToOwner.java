package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;

@ChangeUnit(id = "role-assign-to-owner", order = "027", author = "schub")
public class V027_RoleAssignToOwner {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.ADMINISTRATOR.roleName()),
                Updates.pull("permissions", Permission.USER_ROLE_ASSIGN.name()));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.ADMINISTRATOR.roleName()),
                Updates.addToSet("permissions", Permission.USER_ROLE_ASSIGN.name()));
    }
}
