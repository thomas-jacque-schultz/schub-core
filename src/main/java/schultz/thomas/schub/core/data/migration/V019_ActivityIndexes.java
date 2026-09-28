package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

// Compteurs d'activité et de recherche : 31 jours suffisent à la courbe, rien n'est gardé au-delà.
@ChangeUnit(id = "activity-indexes", order = "019", author = "schub")
public class V019_ActivityIndexes {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("user_activity").createIndex(Indexes.ascending("lastAt"),
                new IndexOptions().name("lastAt_ttl").expireAfter(31L, TimeUnit.DAYS));
        mongoTemplate.getCollection("search_visit").createIndex(Indexes.ascending("at"),
                new IndexOptions().name("at_ttl").expireAfter(31L, TimeUnit.DAYS));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("user_activity").dropIndex("lastAt_ttl");
        mongoTemplate.getCollection("search_visit").dropIndex("at_ttl");
    }
}
