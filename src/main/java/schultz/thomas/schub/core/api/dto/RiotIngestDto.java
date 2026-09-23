package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

public record RiotIngestDto(
        long pending,
        long running,
        Instant estimatedReadyAt
) {
}
