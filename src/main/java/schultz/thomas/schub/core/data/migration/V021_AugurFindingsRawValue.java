package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

// La preuve porte désormais la mesure brute : les constats déjà calculés se recalculent à leur prochaine lecture.
@ChangeUnit(id = "augur-findings-raw-value", order = "021", author = "schub")
public class V021_AugurFindingsRawValue {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.updateMulti(new Query(), new Update().set("stale", true), "augur_findings");
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // Rien à défaire : un constat périmé se recalcule, il n'est pas perdu.
    }
}
