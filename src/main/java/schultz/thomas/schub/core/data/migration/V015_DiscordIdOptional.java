package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import schultz.thomas.schub.core.data.model.User;

// L'identifiant interne devient la clé : un compte peut exister sans Discord.
@ChangeUnit(id = "discord-id-optional", order = "015", author = "schub")
public class V015_DiscordIdOptional {

    private static final String INDEX = "discordId_unique";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var users = mongoTemplate.indexOps(User.class);
        if (users.getIndexInfo().stream().anyMatch(index -> INDEX.equals(index.getName()))) {
            users.dropIndex(INDEX);
        }
        users.ensureIndex(new Index().on("discordId", Sort.Direction.ASC).unique().sparse().named(INDEX));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).dropIndex(INDEX);
        mongoTemplate.indexOps(User.class)
                .ensureIndex(new Index().on("discordId", Sort.Direction.ASC).unique().named(INDEX));
    }
}
