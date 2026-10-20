package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

// backfill : absent si aucun rattrapage n'a été demandé (Schub#113).
public record RiotIngestDto(
        long pending,
        long running,
        @Nullable Instant estimatedReadyAt,
        @Nullable Backfill backfill
) {

    public record Backfill(Instant since, Integer found, long collected) {
    }
}
