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

// Les constats d'équipe (Schub#23 à #25) : duos au-delà ou en deçà de l'attendu, ressources qui paient ou non.
@ChangeUnit(id = "augur-team-patterns", order = "017", author = "schub")
public class V017_AugurTeamPatterns {

    @Execution
    public void execution(MongoTemplate mongo) {
        patterns().forEach(mongo::save);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.remove(Query.query(Criteria.where("scope").is(Scope.TEAM)), PatternVersion.class);
    }

    static List<PatternVersion> patterns() {
        return List.of(
                p("equipe-duo-porteur", Polarity.STRENGTH, Nature.CONSEQUENCE, val("bestDuoDelta", 0.05, 0.15),
                        "Duo porteur", "Winning duo",
                        "{bestDuo} gagnent {bestDuoDelta.points} points de plus qu'attendu ensemble.",
                        "{bestDuo} win {bestDuoDelta.points} points more than expected together."),
                p("equipe-duo-a-revoir", Polarity.WEAKNESS, Nature.CONSEQUENCE, val("worstDuoDelta", -0.05, -0.15),
                        "Duo qui peine", "Struggling duo",
                        "{worstDuo} gagnent {worstDuoDelta.points} points de moins qu'attendu ensemble.",
                        "{worstDuo} win {worstDuoDelta.points} points less than expected together."),
                p("equipe-porteur-ne-convertit-pas", Polarity.WEAKNESS, Nature.ACTION, val("worstConversion", -0.03, -0.10),
                        "Porteur qui ne convertit pas", "Carry not converting",
                        "Votre {position} reçoit plus d'or qu'il ne rend en dégâts : {worstConversion.points} points d'écart.",
                        "Your {position} gets more gold than they return in damage: {worstConversion.points} points apart."),
                p("equipe-ressources-payantes", Polarity.STRENGTH, Nature.CONSEQUENCE, val("bestResourceGap", 0.05, 0.20),
                        "Ressources bien placées", "Resources well placed",
                        "Quand votre {resourcePosition} reçoit l'or, vous gagnez {bestResourceGap.points} points de plus.",
                        "When your {resourcePosition} gets the gold, you win {bestResourceGap.points} points more."));
    }

    private static Condition val(String signal, double from, double to) {
        return new Condition(signal, Condition.Unit.VALUE, from, to, 1);
    }

    private static PatternVersion p(String key, Polarity polarity, Nature nature, Condition requise,
                                    String labelFr, String labelEn, String sentenceFr, String sentenceEn) {
        PatternVersion p = new PatternVersion();
        p.setId(PatternVersion.idOf(key, 1));
        p.setKey(key);
        p.setVersion(1);
        p.setStatus(PatternVersion.Status.ACTIVE);
        p.setScope(Scope.TEAM);
        p.setPolarity(polarity);
        p.setCategory(Category.PERFORMANCE);
        p.setNature(nature);
        p.setRequired(List.of(requise));
        p.setLabel(Map.of("fr", labelFr, "en", labelEn));
        p.setSentence(Map.of("fr", sentenceFr, "en", sentenceEn));
        p.setAuthor("migration");
        p.setComment("Constats d'équipe, revus dans la PR de la v3.1 (Schub#23)");
        p.setCreatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        p.setActivatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        return p;
    }
}
