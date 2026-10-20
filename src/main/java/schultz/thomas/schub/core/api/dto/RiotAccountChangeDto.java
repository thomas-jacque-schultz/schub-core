package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Duration;

public record RiotAccountChangeDto(
        @Nullable String previousRiotId,
        String riotId,
        boolean statsReset,
        boolean ingestRestarted,
        int estimatedMatches,
        @Nullable Duration estimatedDuration,
        boolean rosterSlotsToClaim
) {
}
