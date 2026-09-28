package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.api.dto.UserStatsDto;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Une ligne par jour, par compte et par application ; une par jour et par visiteur de la recherche. Le visiteur n'est
// qu'un hachage salé du jour, fait par le BFF : on compte des personnes distinctes sans pouvoir les reconnaître.
@Service
@RequiredArgsConstructor
public class ActivityService {

    public static final Set<String> APPLICATIONS = Set.of("schub", "premadelab");
    private static final String ACTIVITE = "user_activity";
    private static final String RECHERCHE = "search_visit";
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final int JOURS_COURBE = 30;

    private final MongoTemplate mongo;
    private final UserRepository users;

    public void recordActivity(String userId, String app) {
        String application = APPLICATIONS.contains(app) ? app : "schub";
        Instant maintenant = Instant.now();
        String jour = LocalDate.ofInstant(maintenant, PARIS).toString();
        mongo.findAndModify(Query.query(Criteria.where("_id").is(jour + "|" + userId + "|" + application)),
                new Update().set("day", jour).set("userId", userId).set("app", application).max("lastAt", maintenant),
                FindAndModifyOptions.options().upsert(true), Document.class, ACTIVITE);
    }

    public void recordSearch(String visitor) {
        if (visitor == null || visitor.isBlank() || visitor.length() > 128) {
            return;
        }
        Instant maintenant = Instant.now();
        String jour = LocalDate.ofInstant(maintenant, PARIS).toString();
        mongo.upsert(Query.query(Criteria.where("_id").is(jour + "|" + visitor)),
                new Update().setOnInsert("day", jour).setOnInsert("at", maintenant), RECHERCHE);
    }

    public UserStatsDto stats() {
        Instant maintenant = Instant.now();
        Map<String, UserStatsDto.Active> parApplication = new LinkedHashMap<>();
        for (String app : List.of("schub", "premadelab")) {
            parApplication.put(app, actifs(maintenant, app));
        }
        return new UserStatsDto(users.count(), users.countByRiotPuuidNotNull(), actifs(maintenant, null),
                parApplication, courbe(maintenant));
    }

    private UserStatsDto.Active actifs(Instant maintenant, String app) {
        return new UserStatsDto.Active(
                distincts(maintenant.minus(Duration.ofHours(24)), app),
                distincts(maintenant.minus(Duration.ofDays(7)), app),
                distincts(maintenant.minus(Duration.ofDays(30)), app));
    }

    private long distincts(Instant depuis, String app) {
        Criteria critere = Criteria.where("lastAt").gte(depuis);
        if (app != null) {
            critere = critere.and("app").is(app);
        }
        return mongo.findDistinct(Query.query(critere), "userId", ACTIVITE, String.class).size();
    }

    private List<UserStatsDto.Day> courbe(Instant maintenant) {
        LocalDate aujourdhui = LocalDate.ofInstant(maintenant, PARIS);
        String premier = aujourdhui.minusDays(JOURS_COURBE - 1L).toString();

        Map<String, Long> actifs = parJour(mongo.getCollection(ACTIVITE).aggregate(List.of(
                new Document("$match", new Document("day", new Document("$gte", premier))),
                new Document("$group", new Document("_id", new Document("day", "$day").append("u", "$userId"))),
                new Document("$group", new Document("_id", "$_id.day").append("n", new Document("$sum", 1))))));
        Map<String, Long> chercheurs = parJour(mongo.getCollection(RECHERCHE).aggregate(List.of(
                new Document("$match", new Document("day", new Document("$gte", premier))),
                new Document("$group", new Document("_id", "$day").append("n", new Document("$sum", 1))))));

        List<UserStatsDto.Day> jours = new ArrayList<>();
        for (int i = JOURS_COURBE - 1; i >= 0; i--) {
            String jour = aujourdhui.minusDays(i).toString();
            jours.add(new UserStatsDto.Day(jour, actifs.getOrDefault(jour, 0L), chercheurs.getOrDefault(jour, 0L)));
        }
        return jours;
    }

    private static Map<String, Long> parJour(Iterable<Document> lignes) {
        Map<String, Long> parJour = new LinkedHashMap<>();
        lignes.forEach(ligne -> parJour.put(ligne.getString("_id"), ((Number) ligne.get("n")).longValue()));
        return parJour;
    }
}
