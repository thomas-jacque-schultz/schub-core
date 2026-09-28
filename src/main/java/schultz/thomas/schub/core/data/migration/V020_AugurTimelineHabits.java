package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Category;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Nature;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Polarity;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Scope;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Habitudes de timeline (Schub#41). Seuils : 75e et 90e centiles des moyennes de 76 joueurs de l'échantillon par palier,
// mesurés le 28/09 (Schub/docs/rd/README.md). Rien sous cinq parties avec timeline.
@ChangeUnit(id = "augur-timeline-habits", order = "020", author = "schub")
public class V020_AugurTimelineHabits {

    @Execution
    public void execution(MongoTemplate mongo) {
        patterns().forEach(mongo::save);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        patterns().forEach(p -> mongo.remove(Query.query(Criteria.where("_id").is(p.getId())), PatternVersion.class));
    }

    static List<PatternVersion> patterns() {
        return List.of(
                p("habitude-morts-isolees", Category.BEHAVIOUR,
                        val("isolatedDeathsAvg", 0.6, 0.9),
                        "Meurt souvent isolé", "Often dies alone",
                        "{isolatedDeathsAvg.value} mort sans allié à moins de 2 000 unités par partie, sur {timelineGames.value} parties analysées : plus que les trois quarts des joueurs.",
                        "{isolatedDeathsAvg.value} death with no ally within 2,000 units per game, over {timelineGames.value} analysed games: more than three quarters of players."),
                p("habitude-premier-mort", Category.BEHAVIOUR,
                        val("firstDeathsInFightsAvg", 0.75, 1.1),
                        "Souvent le premier à tomber", "Often the first to fall",
                        "Premier mort de {firstDeathsInFightsAvg.value} combat par partie, sur {timelineGames.value} parties analysées : le camp qui perd son premier joueur ne gagne qu'un combat sur quatre.",
                        "First death in {firstDeathsInFightsAvg.value} fight per game, over {timelineGames.value} analysed games: the side that loses its first player wins one fight in four."),
                p("habitude-disperses-aux-objectifs", Category.BEHAVIOUR,
                        val("groupedAtObjectivesShareAvg", 0.2, 0.15),
                        "Arrive dispersé aux objectifs", "Scattered at objectives",
                        "Trois alliés ou plus autour des dragons et des barons seulement {groupedAtObjectivesShareAvg.points} % du temps, sur {timelineGames.value} parties analysées.",
                        "Three allies or more around dragons and barons only {groupedAtObjectivesShareAvg.points}% of the time, over {timelineGames.value} analysed games."));
    }

    private static Condition val(String signal, double from, double to) {
        return new Condition(signal, Condition.Unit.VALUE, from, to, 1);
    }

    private static PatternVersion p(String key, Category category, Condition signal,
                                    String labelFr, String labelEn, String sentenceFr, String sentenceEn) {
        PatternVersion p = new PatternVersion();
        p.setId(PatternVersion.idOf(key, 1));
        p.setKey(key);
        p.setVersion(1);
        p.setStatus(PatternVersion.Status.ACTIVE);
        p.setScope(Scope.HABIT);
        p.setPolarity(Polarity.WEAKNESS);
        p.setCategory(category);
        p.setNature(Nature.ACTION);
        p.setRequired(List.of(val("timelineGames", 4, 5), signal));
        p.setExceptions(List.of());
        p.setLabel(Map.of("fr", labelFr, "en", labelEn));
        p.setSentence(Map.of("fr", sentenceFr, "en", sentenceEn));
        p.setAuthor("migration");
        p.setComment("Schub#41 : habitudes de timeline, seuils mesurés sur l'échantillon par palier");
        p.setCreatedAt(Instant.parse("2026-09-28T00:00:00Z"));
        p.setActivatedAt(Instant.parse("2026-09-28T00:00:00Z"));
        return p;
    }
}
