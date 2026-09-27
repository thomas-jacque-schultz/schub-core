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

// Les constats de timeline (R&D Schub#14 et #26) et de build (R&D Schub#19), sur les capteurs du connecteur Riot.
@ChangeUnit(id = "augur-timeline-build", order = "018", author = "schub")
public class V018_AugurTimelineAndBuild {

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
                p("partie-morts-isolees", Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                        List.of(val("isolatedDeaths", 0.5, 2.5)), List.of(),
                        "Morts isolées", "Isolated deaths",
                        "{isolatedDeaths.value} mort(s) sans aucun allié à moins de 2 000 unités.",
                        "{isolatedDeaths.value} death(s) with no ally within 2,000 units."),
                p("partie-premier-mort", Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                        List.of(val("firstDeathsInFights", 0.5, 2.5)), List.of(),
                        "Premier à tomber", "First to fall",
                        "Premier mort de {firstDeathsInFights.value} combat(s) : le camp qui perd son premier joueur ne gagne qu'un combat sur quatre.",
                        "First death in {firstDeathsInFights.value} fight(s): the side that loses its first player wins one fight in four."),
                p("partie-morts-en-chaine", Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.CONSEQUENCE,
                        List.of(val("chainDeathShare", 0.4, 0.7)), List.of(),
                        "Morts en chaîne", "Chain deaths",
                        "{chainDeathShare.points} % des morts de l'équipe suivies d'une autre dans les 30 s : les combats se perdent en cascade.",
                        "{chainDeathShare.points}% of the team's deaths followed by another within 30 s: fights are lost in a cascade."),
                p("partie-objectifs-cedes", Polarity.WEAKNESS, Category.KNOWLEDGE, Nature.CONSEQUENCE,
                        List.of(val("objectivesCededWithoutTrade", 0.5, 2.5)), List.of(),
                        "Objectifs cédés sans contrepartie", "Objectives given away",
                        "{objectivesCededWithoutTrade.value} objectif(s) laissé(s) à l'adversaire sans rien prendre ailleurs dans la minute.",
                        "{objectivesCededWithoutTrade.value} objective(s) left to the enemy without taking anything else within the minute."),
                p("partie-disperses-aux-objectifs", Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                        List.of(val("groupedAtObjectivesShare", 0.5, 0.1)), List.of(),
                        "Dispersés aux objectifs", "Scattered at objectives",
                        "Trois membres ou plus autour des dragons et des barons seulement {groupedAtObjectivesShare.points} % du temps.",
                        "Three members or more around dragons and barons only {groupedAtObjectivesShare.points}% of the time."),
                p("partie-engage-en-inferiorite", Polarity.WEAKNESS, Category.BEHAVIOUR, Nature.ACTION,
                        List.of(val("outnumberedFights", 0.5, 2.5)), List.of(),
                        "Engage en infériorité", "Fights outnumbered",
                        "{outnumberedFights.value} combat(s) engagé(s) à moins nombreux : à peine plus d'un sur quatre se gagne.",
                        "{outnumberedFights.value} fight(s) taken outnumbered: barely one in four is won."),
                p("partie-morts-en-gank", Polarity.WEAKNESS, Category.KNOWLEDGE, Nature.ACTION,
                        List.of(val("gankDeaths", 0.5, 2.5)), List.of(),
                        "Victime des ganks", "Caught by ganks",
                        "{gankDeaths.value} mort(s) sur un gank avant 15 minutes.",
                        "{gankDeaths.value} death(s) to a gank before 15 minutes."),
                p("partie-echanges-perdus", Polarity.WEAKNESS, Category.PERFORMANCE, Nature.ACTION,
                        List.of(val("lostTradeDeaths", 0.5, 2.5)), List.of(),
                        "Duels perdus", "Lost duels",
                        "{lostTradeDeaths.value} mort(s) en duel face à ton adversaire direct.",
                        "{lostTradeDeaths.value} death(s) in a duel against your direct opponent."),
                p("build-armure-contre-critique", Polarity.WEAKNESS, Category.BUILD, Nature.ACTION,
                        List.of(val("enemyCritCarries", 1, 2), val("frontlineArmored", 0, 1)),
                        List.of(val("hasAntiCrit", 0, 1)),
                        "Armure sans anti-critique", "Armor without anti-crit",
                        "{enemyCritCarries.value} porteurs de critique en face, et ni Présage de Randuin ni Tabi ninja dans ton build.",
                        "{enemyCritCarries.value} critical strike carries on the enemy team, and neither Randuin's Omen nor Ninja Tabi in your build."),
                p("build-resistance-magique", Polarity.WEAKNESS, Category.BUILD, Nature.ACTION,
                        List.of(val("magicDamageShare", 0.6, 0.7), val("armorOverMr", 0, 1), val("resistancesTotal", 40, 60)),
                        List.of(),
                        "Résistance mal choisie", "Wrong resistance",
                        "{magicDamageShare.points} % des dégâts subis étaient magiques, et ton build prend plus d'armure que de résistance magique.",
                        "{magicDamageShare.points}% of the damage taken was magic, and your build favours armor over magic resist."));
    }

    private static Condition val(String signal, double from, double to) {
        return new Condition(signal, Condition.Unit.VALUE, from, to, 1);
    }

    private static PatternVersion p(String key, Polarity polarity, Category category, Nature nature,
                                    List<Condition> requises, List<Condition> exceptions,
                                    String labelFr, String labelEn, String sentenceFr, String sentenceEn) {
        PatternVersion p = new PatternVersion();
        p.setId(PatternVersion.idOf(key, 1));
        p.setKey(key);
        p.setVersion(1);
        p.setStatus(PatternVersion.Status.ACTIVE);
        p.setScope(Scope.GAME);
        p.setPolarity(polarity);
        p.setCategory(category);
        p.setNature(nature);
        p.setRequired(requises);
        p.setExceptions(exceptions);
        p.setLabel(Map.of("fr", labelFr, "en", labelEn));
        p.setSentence(Map.of("fr", sentenceFr, "en", sentenceEn));
        p.setAuthor("migration");
        p.setComment("Conclusions des R&D #14, #19 et #26, revues dans la PR de la v3.1 (Schub/docs/rd)");
        p.setCreatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        p.setActivatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        return p;
    }
}
