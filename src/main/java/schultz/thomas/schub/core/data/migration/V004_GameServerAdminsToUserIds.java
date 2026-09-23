package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChangeUnit(id = "gameserver-admins-to-user-ids", order = "004", author = "schub")
public class V004_GameServerAdminsToUserIds {

    private static final Logger log = LoggerFactory.getLogger(V004_GameServerAdminsToUserIds.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        Map<String, String> idsByUsername = new HashMap<>();
        List<String> knownIds = new ArrayList<>();
        for (Document user : mongoTemplate.getCollection("users").find()) {
            String id = String.valueOf(user.get("_id"));
            knownIds.add(id);
            String username = user.getString("discordUsername");
            if (username != null && !username.isBlank()) {
                idsByUsername.put(username.toLowerCase(), id);
            }
        }

        for (Document server : mongoTemplate.getCollection("servers").find()) {
            List<?> admins = server.getList("admins", Object.class);
            if (admins == null || admins.isEmpty()) {
                continue;
            }
            String slug = server.getString("slug");
            List<String> resolved = new ArrayList<>();
            for (Object raw : admins) {
                String value = String.valueOf(raw);
                if (knownIds.contains(value)) {
                    resolved.add(value);
                    continue;
                }
                String matched = idsByUsername.get(value.toLowerCase());
                if (matched != null) {
                    resolved.add(matched);
                    log.info("Serveur '{}' : administrateur « {} » résolu en compte {}", slug, value, matched);
                } else {
                    log.warn("Serveur '{}' : administrateur « {} » ne correspond à aucun compte — retiré", slug, value);
                }
            }
            if (!resolved.equals(admins)) {
                mongoTemplate.getCollection("servers").updateOne(
                        Filters.eq("_id", asId(server.get("_id"))),
                        Updates.set("admins", resolved));
            }
        }
    }

    private Object asId(Object id) {
        return id instanceof ObjectId objectId ? objectId : id;
    }

    @RollbackExecution
    public void rollback() {
        log.warn("Retour en arrière impossible : les pseudos d'administrateurs d'origine ne sont pas conservés");
    }
}
