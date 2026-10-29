package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

// Plans du Macro Planner (Schub#172) : lus par équipe, le plus récent d'abord.
@ChangeUnit(id = "macro-plan-indexes", order = "030", author = "schub")
public class V030_MacroPlanIndexes {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("team_macro_plans").createIndex(
                Indexes.compoundIndex(Indexes.ascending("teamId"), Indexes.descending("updatedAt")),
                new IndexOptions().name("teamId_updatedAt"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("team_macro_plans").dropIndex("teamId_updatedAt");
    }
}
