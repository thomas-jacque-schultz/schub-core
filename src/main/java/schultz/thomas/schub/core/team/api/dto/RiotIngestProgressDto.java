package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

public record RiotIngestProgressDto(long pending, long running, Instant estimatedReadyAt) {
}
