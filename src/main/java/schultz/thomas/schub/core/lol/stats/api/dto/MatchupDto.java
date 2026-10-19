package schultz.thomas.schub.core.lol.stats.api.dto;


// rankGap : divisions, notre joueur moins son vis-à-vis ; solo à défaut flex, du même côté des deux.
public record MatchupDto(String position, TeamGamePlayerDto ally, TeamGamePlayerDto enemy, Double rankGap) {
}
