package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;

public record RiotIngestProgress(long pending, long running, Instant estimatedReadyAt) {
}
