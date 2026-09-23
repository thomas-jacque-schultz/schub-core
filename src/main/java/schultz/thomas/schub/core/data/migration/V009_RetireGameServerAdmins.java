package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "retire-gameserver-admins", order = "009", author = "schub")
public class V009_RetireGameServerAdmins {

    private static final Logger log = LoggerFactory.getLogger(V009_RetireGameServerAdmins.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        UpdateResult result = mongoTemplate.getCollection("servers").updateMany(
                Filters.exists("admins"),
                Updates.unset("admins"));
        log.info("Administrateurs par serveur retirés de {} document(s)", result.getModifiedCount());
    }

    @RollbackExecution
    public void rollback() {
        log.warn("Retour en arrière sans objet : l'autorité par serveur n'existe plus");
    }
}
