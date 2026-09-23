package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import schultz.thomas.schub.core.data.model.User;

@ChangeUnit(id = "riot-puuid-unique-index", order = "007", author = "schub")
public class V007_RiotPuuidUniqueIndex {

    private static final String NOM = "riotPuuid_unique";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).ensureIndex(
                new Index()
                        .on("riotPuuid", Sort.Direction.ASC)
                        .unique()
                        .named(NOM)
                        .partial(PartialIndexFilter.of(
                                new Document("riotPuuid", new Document("$type", "string")))));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).dropIndex(NOM);
    }
}
