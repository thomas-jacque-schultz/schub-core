package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.FindingRecord;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Category;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Nature;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Polarity;
import schultz.thomas.schub.core.augur.data.model.PatternVersion.Scope;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Les premiers patterns du moteur Augur (Schub#37). Les suivants s'écrivent dans l'éditeur de Schub.
@ChangeUnit(id = "augur-seed", order = "016", author = "schub")
public class V016_AugurSeed {

    @Execution
    public void execution(MongoTemplate mongo) {
        mongo.indexOps(PatternVersion.class).ensureIndex(new Index()
                .on("key", Sort.Direction.ASC).on("version", Sort.Direction.ASC).unique().named("key_version_unique"));
        mongo.indexOps(FindingRecord.class).ensureIndex(new Index().on("subject", Sort.Direction.ASC).named("subject"));
        mongo.indexOps(FindingRecord.class).ensureIndex(new Index().on("patternKey", Sort.Direction.ASC).named("patternKey"));

        patterns().forEach(mongo::save);

        mongo.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.OWNER.roleName()),
                Updates.addToSet("permissions", Permission.AUGUR_PATTERN_EDIT.name()));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.dropCollection(PatternVersion.class);
        mongo.dropCollection(FindingRecord.class);
        mongo.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.OWNER.roleName()),
                Updates.pull("permissions", Permission.AUGUR_PATTERN_EDIT.name()));
    }

    static List<PatternVersion> patterns() {
        List<PatternVersion> l = new ArrayList<>();

        l.add(p("partie-morts-nombreuses", Scope.GAME, Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("deathsPer10", 60, 85)), List.of(pct("timeDeadShare", 60, 85)), List.of(),
                "Beaucoup de morts", "Many deaths",
                "{deathsPer10.value} morts par tranche de 10 minutes : plus que {deathsPer10.percentile} % des parties à ce poste et à ce palier.",
                "{deathsPer10.value} deaths per 10 minutes: more than {deathsPer10.percentile}% of games in this role and tier."));
        l.add(p("partie-vision-faible", Scope.GAME, Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("visionPerMinute", 35, 10)), List.of(pct("controlWardsPlaced", 35, 10)), List.of(),
                "Vision faible", "Low vision",
                "Score de vision de {visionPerMinute.value} par minute, au {visionPerMinute.percentile}e centile de ce poste et de ce palier.",
                "Vision score of {visionPerMinute.value} per minute, at the {visionPerMinute.percentile}th percentile for this role and tier."));
        l.add(p("partie-farm-solide", Scope.GAME, Polarity.STRENGTH, Category.PERFORMANCE, Nature.ACTION,
                List.of(pct("csPerMinute", 65, 90)), List.of(), List.of(val("isSupport", 0, 1)),
                "Farm solide", "Solid farming",
                "{csPerMinute.value} sbires par minute : mieux que {csPerMinute.percentile} % des parties à ce poste et à ce palier.",
                "{csPerMinute.value} CS per minute: better than {csPerMinute.percentile}% of games in this role and tier."));
        l.add(p("partie-lane-gagnee", Scope.GAME, Polarity.STRENGTH, Category.PERFORMANCE, Nature.CONSEQUENCE,
                List.of(val("goldDiffAt15", 0, 1500)), List.of(val("csDiffAt15", 0, 20)), List.of(),
                "Phase de lane gagnée", "Lane won",
                "{goldDiffAt15.value} pièces d'or d'avance sur ton adversaire direct à 15 minutes.",
                "{goldDiffAt15.value} gold ahead of your direct opponent at 15 minutes."));
        l.add(p("partie-degats-porteur", Scope.GAME, Polarity.STRENGTH, Category.PERFORMANCE, Nature.CONSEQUENCE,
                List.of(pct("damageShare", 65, 90)), List.of(), List.of(),
                "Porteur des dégâts", "Damage carry",
                "Une part des dégâts de l'équipe plus grande que dans {damageShare.percentile} % des parties à ce poste.",
                "A larger share of the team's damage than in {damageShare.percentile}% of games in this role."));
        l.add(p("partie-style-agressif", Scope.GAME, Polarity.NEUTRAL, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("killParticipation", 55, 80)), List.of(pct("damagePerMinute", 55, 80)), List.of(),
                "Au cœur des combats", "In every fight",
                "Présent sur la plupart des éliminations de l'équipe.",
                "Involved in most of the team's kills."));

        l.add(p("habitude-meurt-souvent", Scope.HABIT, Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("deathsPer10", 60, 85)), List.of(), List.of(),
                "Meurt souvent", "Dies often",
                "En moyenne {deathsPer10.value} morts par tranche de 10 minutes : plus que {deathsPer10.percentile} % des joueurs de ce poste et de ce palier.",
                "On average {deathsPer10.value} deaths per 10 minutes: more than {deathsPer10.percentile}% of players in this role and tier."));
        l.add(p("habitude-vision-faible", Scope.HABIT, Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("visionPerMinute", 35, 10)), List.of(pct("controlWardsPlaced", 35, 10)), List.of(),
                "Vision faible", "Low vision",
                "Score de vision moyen au {visionPerMinute.percentile}e centile des joueurs de ce poste et de ce palier.",
                "Average vision score at the {visionPerMinute.percentile}th percentile of players in this role and tier."));
        l.add(p("habitude-farm-regulier", Scope.HABIT, Polarity.STRENGTH, Category.PERFORMANCE, Nature.ACTION,
                List.of(pct("csPerMinute", 60, 85)), List.of(), List.of(val("isSupport", 0, 1)),
                "Farm régulier", "Consistent farming",
                "{csPerMinute.value} sbires par minute en moyenne : mieux que {csPerMinute.percentile} % des joueurs de ce poste et de ce palier.",
                "{csPerMinute.value} CS per minute on average: better than {csPerMinute.percentile}% of players in this role and tier."));
        l.add(p("habitude-joueur-equipe", Scope.HABIT, Polarity.STRENGTH, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("killParticipation", 60, 85)), List.of(), List.of(),
                "Joueur d'équipe", "Team player",
                "Participe à plus d'éliminations de son équipe que {killParticipation.percentile} % des joueurs de ce poste.",
                "Takes part in more of the team's kills than {killParticipation.percentile}% of players in this role."));
        l.add(p("habitude-lane-dominante", Scope.HABIT, Polarity.STRENGTH, Category.PERFORMANCE, Nature.CONSEQUENCE,
                List.of(val("goldDiffAt15", 0, 1000)), List.of(), List.of(),
                "Phase de lane dominante", "Dominant laning",
                "En moyenne {goldDiffAt15.value} pièces d'or d'avance à 15 minutes sur ton adversaire direct.",
                "On average {goldDiffAt15.value} gold ahead of your direct opponent at 15 minutes."));
        l.add(p("habitude-lane-difficile", Scope.HABIT, Polarity.WEAKNESS, Category.PERFORMANCE, Nature.CONSEQUENCE,
                List.of(val("goldDiffAt15", 0, -1000)), List.of(), List.of(),
                "Phase de lane difficile", "Tough laning",
                "En moyenne {goldDiffAt15.value} pièces d'or à 15 minutes face à ton adversaire direct.",
                "On average {goldDiffAt15.value} gold at 15 minutes against your direct opponent."));
        l.add(p("habitude-joue-sans-information", Scope.HABIT, Polarity.WEAKNESS, Category.KNOWLEDGE, Nature.ACTION,
                List.of(val("pattern:habitude-vision-faible", 0.3, 0.7), val("pattern:habitude-meurt-souvent", 0.3, 0.7)),
                List.of(), List.of(),
                "Joue sans information", "Plays blind",
                "Peu de vision et beaucoup de morts : tu te déplaces sans savoir où sont les adversaires.",
                "Little vision and many deaths: you move around without knowing where the enemies are."));
        l.add(p("habitude-style-agressif", Scope.HABIT, Polarity.NEUTRAL, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("damagePerMinute", 55, 80)), List.of(pct("killParticipation", 55, 80)), List.of(),
                "Style agressif", "Aggressive style",
                "Inflige plus de dégâts par minute que {damagePerMinute.percentile} % des joueurs de ce poste.",
                "Deals more damage per minute than {damagePerMinute.percentile}% of players in this role."));
        l.add(p("habitude-style-prudent", Scope.HABIT, Polarity.NEUTRAL, Category.BEHAVIOUR, Nature.ACTION,
                List.of(pct("deathsPer10", 40, 15)), List.of(), List.of(),
                "Style prudent", "Careful style",
                "Meurt moins que {deathsPer10.percentile} % des joueurs de ce poste et de ce palier.",
                "Dies less than {deathsPer10.percentile}% of players in this role and tier."));
        l.add(p("habitude-porteur", Scope.HABIT, Polarity.NEUTRAL, Category.PERFORMANCE, Nature.CONSEQUENCE,
                List.of(pct("damageShare", 60, 85)), List.of(), List.of(),
                "Porte son équipe", "Carries the team",
                "Assure une plus grande part des dégâts de son équipe que {damageShare.percentile} % des joueurs de ce poste.",
                "Deals a larger share of the team's damage than {damageShare.percentile}% of players in this role."));
        return l;
    }

    private static Condition pct(String signal, double from, double to) {
        return new Condition(signal, Condition.Unit.PERCENTILE, from, to, 1);
    }

    private static Condition val(String signal, double from, double to) {
        return new Condition(signal, Condition.Unit.VALUE, from, to, 1);
    }

    private static PatternVersion p(String key, Scope scope, Polarity polarity, Category category, Nature nature,
                                    List<Condition> required, List<Condition> optional, List<Condition> exceptions,
                                    String labelFr, String labelEn, String sentenceFr, String sentenceEn) {
        PatternVersion p = new PatternVersion();
        p.setId(PatternVersion.idOf(key, 1));
        p.setKey(key);
        p.setVersion(1);
        p.setStatus(PatternVersion.Status.ACTIVE);
        p.setScope(scope);
        p.setPolarity(polarity);
        p.setCategory(category);
        p.setNature(nature);
        p.setRequired(required);
        p.setOptional(optional);
        p.setExceptions(exceptions);
        p.setLabel(Map.of("fr", labelFr, "en", labelEn));
        p.setSentence(Map.of("fr", sentenceFr, "en", sentenceEn));
        p.setAuthor("migration");
        p.setComment("Premiers patterns, revus dans la PR de la v3.1 (Schub#37)");
        p.setCreatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        p.setActivatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        return p;
    }
}
