package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record RiotIngestProgressDto(long pending, long running, @Nullable Instant estimatedReadyAt) {
}
