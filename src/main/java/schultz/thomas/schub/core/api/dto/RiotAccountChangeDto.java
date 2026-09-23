package schultz.thomas.schub.core.api.dto;

import java.time.Duration;

public record RiotAccountChangeDto(
        String previousRiotId,
        String riotId,
        boolean statsReset,
        boolean ingestRestarted,
        int estimatedMatches,
        Duration estimatedDuration,
        boolean rosterSlotsToClaim
) {
}
