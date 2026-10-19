package schultz.thomas.schub.core.team.business.model;

// rankGap : divisions, notre joueur moins son vis-à-vis ; solo à défaut flex, du même côté des deux.
public record Matchup(String position, TeamGamePlayer ally, TeamGamePlayer enemy, Double rankGap) {
}
