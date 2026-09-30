package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;

// Limites relevées par la R&D (Schub/docs/rd/README.md). Toutes les versions d'une règle, brouillon compris.
@ChangeUnit(id = "augur-experimental", order = "023", author = "schub")
public class V023_AugurExperimental {

    private static final Map<String, String> MINUTE = Map.of(
            "fr", "La position des joueurs n'est connue qu'une fois par minute : seule une mort sur huit peut être jugée, les autres ne sont pas comptées.",
            "en", "Player positions are only known once a minute: only one death in eight can be judged, the others are not counted.");

    private static final Map<String, String> SEUILS_COMMUNS = Map.of(
            "fr", "Seuils communs à tous les paliers, tirés de 76 joueurs : pas encore de grille par palier.",
            "en", "Thresholds shared by every tier, drawn from 76 players: no per-tier grid yet.");

    private static final Map<String, Map<String, String>> LIMITES = Map.of(
            "partie-morts-isolees", MINUTE,
            "habitude-morts-isolees", Map.of(
                    "fr", MINUTE.get("fr") + " " + SEUILS_COMMUNS.get("fr"),
                    "en", MINUTE.get("en") + " " + SEUILS_COMMUNS.get("en")),
            "habitude-premier-mort", SEUILS_COMMUNS,
            "habitude-disperses-aux-objectifs", SEUILS_COMMUNS,
            "partie-objectifs-cedes", Map.of(
                    "fr", "Seuil posé sans grille par palier ; la contrepartie n'est cherchée que dans la minute qui suit.",
                    "en", "Threshold set without a per-tier grid; a trade is only looked for within the following minute."));

    @Execution
    public void execution(MongoTemplate mongo) {
        var patterns = mongo.getCollection("augur_patterns");
        LIMITES.forEach((key, limites) -> patterns.updateMany(Filters.eq("key", key),
                Updates.combine(Updates.set("experimental", true), Updates.set("limits", new Document(limites)))));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.getCollection("augur_patterns").updateMany(Filters.in("key", LIMITES.keySet()),
                Updates.combine(Updates.unset("experimental"), Updates.unset("limits")));
    }
}
