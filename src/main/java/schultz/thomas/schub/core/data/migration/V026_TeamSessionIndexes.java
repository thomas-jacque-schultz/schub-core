package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

// Sessions d'équipe (Schub#103) : lues par équipe et par jour, du plus récent au plus ancien.
@ChangeUnit(id = "team-session-indexes", order = "026", author = "schub")
public class V026_TeamSessionIndexes {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("team_sessions").createIndex(
                Indexes.compoundIndex(Indexes.ascending("teamId"), Indexes.descending("day")),
                new IndexOptions().name("teamId_day"));
        mongoTemplate.getCollection("team_sessions").createIndex(Indexes.ascending("day"),
                new IndexOptions().name("day"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("team_sessions").dropIndex("teamId_day");
        mongoTemplate.getCollection("team_sessions").dropIndex("day");
    }
}
