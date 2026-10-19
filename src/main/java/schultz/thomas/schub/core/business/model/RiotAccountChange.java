package schultz.thomas.schub.core.business.model;

import java.time.Duration;

public record RiotAccountChange(
        String previousRiotId,
        String riotId,
        boolean statsReset,
        boolean ingestRestarted,
        int estimatedMatches,
        Duration estimatedDuration,
        boolean rosterSlotsToClaim
) {
}
