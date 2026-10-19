package schultz.thomas.schub.core.business.model;

import java.time.Instant;
import java.util.List;

public record RiotAccountSuggestion(
        String riotId,
        String gameName,
        String tagLine,
        long matchCount,
        List<PositionPlayed> positions,
        Instant lastPlayedAt,
        Instant observedAt,
        String source,
        boolean alreadyLinked,
        boolean mine
) {

    public record PositionPlayed(String position, long matches) {
    }
}
