package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.RiotAccountState;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record RiotAccountDto(
        RiotAccountState state,
        @Nullable String riotId,
        @Nullable String gameName,
        @Nullable String tagLine,
        @Nullable Instant linkedAt,
        @Nullable RiotIngestDto ingest,
        RiotAccountChangeDto change
) {
}
