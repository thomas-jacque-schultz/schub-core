package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

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
