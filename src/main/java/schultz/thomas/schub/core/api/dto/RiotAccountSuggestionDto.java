package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

public record RiotAccountSuggestionDto(
        String riotId,
        String gameName,
        String tagLine,
        long matchCount,
        List<PositionPlayedDto> positions,
        Instant lastPlayedAt,
        Instant observedAt,
        String source,
        boolean alreadyLinked,
        boolean mine
) {

    public record PositionPlayedDto(String position, long matches) {
    }
}
