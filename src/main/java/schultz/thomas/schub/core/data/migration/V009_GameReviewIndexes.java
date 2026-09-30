package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;

@ChangeUnit(id = "game-review-indexes", order = "009", author = "schub")
public class V009_GameReviewIndexes {

    // La revue est retirée (Schub#67), sa collection reste en base : le nom remplace la classe.
    private static final String COLLECTION = "team_game_reviews";
    private static final String UNIQUE = "revue_unique";
    private static final String PAR_PARTIE = "revue_equipe_partie";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new CompoundIndexDefinition(new Document()
                        .append("matchId", 1)
                        .append("subjectMemberId", 1)
                        .append("authorUserId", 1))
                        .unique()
                        .named(UNIQUE));

        mongoTemplate.indexOps(COLLECTION).ensureIndex(
                new CompoundIndexDefinition(new Document()
                        .append("teamId", 1)
                        .append("matchId", 1)
                        .append("createdAt", 1))
                        .named(PAR_PARTIE));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(COLLECTION).dropIndex(UNIQUE);
        mongoTemplate.indexOps(COLLECTION).dropIndex(PAR_PARTIE);
    }
}
