package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * L'unicité protocole + port WAN n'a jamais existé.
 *
 * <p>{@code StaticPortRuleEntity} porte {@code @CompoundIndex(unique = true)} depuis toujours, et
 * l'annotation est inerte : {@code auto-index-creation} vaut {@code false} par défaut. Vérifié en
 * base — la collection ne portait que {@code _id_}. Deux règles sur le même port étaient donc
 * acceptées, et le conflit se découvrait devant la box, ce que cette contrainte devait empêcher.
 *
 * <p>Quatrième occurrence du même piège après V001, V002, V007 et V009.</p>
 */
@ChangeUnit(id = "static-port-rule-unique-index", order = "011", author = "schub")
public class V011_StaticPortRuleUniqueIndex {

    private static final Logger log = LoggerFactory.getLogger(V011_StaticPortRuleUniqueIndex.class);
    private static final String COLLECTION = "static_port_rules";
    private static final String INDEX = "proto_wan_unique";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        List<Document> doublons = doublons(mongoTemplate);
        if (!doublons.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de poser l'unicité protocole + port WAN : " + doublons.size()
                            + " doublon(s) en base. Supprimez-les puis relancez. " + doublons);
        }

        mongoTemplate.getCollection(COLLECTION).createIndex(
                Indexes.ascending("proto", "wanPortStart"),
                new IndexOptions().name(INDEX).unique(true));
        log.info("Index {} posé sur {}", INDEX, COLLECTION);
    }

    private List<Document> doublons(MongoTemplate mongoTemplate) {
        List<Document> pipeline = List.of(
                new Document("$group", new Document("_id",
                        new Document("proto", "$proto").append("wanPortStart", "$wanPortStart"))
                        .append("n", new Document("$sum", 1))),
                new Document("$match", new Document("n", new Document("$gt", 1))));

        List<Document> trouves = new ArrayList<>();
        mongoTemplate.getCollection(COLLECTION).aggregate(pipeline).forEach(trouves::add);
        return trouves;
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection(COLLECTION).dropIndex(INDEX);
    }
}
