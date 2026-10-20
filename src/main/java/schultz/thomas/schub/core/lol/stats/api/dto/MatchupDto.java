package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

// rankGap : divisions, notre joueur moins son vis-à-vis ; solo à défaut flex, du même côté des deux.
public record MatchupDto(String position, @Nullable TeamGamePlayerDto ally, @Nullable TeamGamePlayerDto enemy,
                         @Nullable Double rankGap) {
}
