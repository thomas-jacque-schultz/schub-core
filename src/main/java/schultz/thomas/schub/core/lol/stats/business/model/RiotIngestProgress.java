package schultz.thomas.schub.core.lol.stats.business.model;

import java.time.Instant;

public record RiotIngestProgress(long pending, long running, Instant estimatedReadyAt) {
}
