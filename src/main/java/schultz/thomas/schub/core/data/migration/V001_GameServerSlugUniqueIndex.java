package schultz.thomas.schub.core.data.migration;

import schultz.thomas.schub.core.data.model.GameServer;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
@ChangeUnit(id = "gameserver-slug-unique-index", order = "001", author = "schub")
public class V001_GameServerSlugUniqueIndex {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(GameServer.class)
                .ensureIndex(new Index()
                        .on("slug", Sort.Direction.ASC)
                        .unique()
                        .named("slug_unique"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(GameServer.class).dropIndex("slug_unique");
    }
}
