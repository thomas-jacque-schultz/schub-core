package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Une partie d'équipe.
 *
 * @param queueId  conservé et affiché : une victoire en normale draft ne vaut pas une victoire en
 *                 flex. Il ne filtre rien pour autant.
 * @param win      nul quand les membres présents n'étaient pas du même côté — il n'y a alors pas
 *                 de résultat d'équipe, et en inventer un serait faux dans les deux sens.
 */
public record TeamGameDto(
        String matchId,
        Instant startedAt,
        long durationSeconds,
        int queueId,
        String queue,
        String patch,
        int presentPlayers,
        boolean splitSides,
        Boolean win,
        List<TeamGamePlayerDto> players
) {
}
