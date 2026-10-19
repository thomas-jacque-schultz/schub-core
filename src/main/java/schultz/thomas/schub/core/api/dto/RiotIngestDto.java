package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

// backfill : absent si aucun rattrapage n'a été demandé (Schub#113).
public record RiotIngestDto(
        long pending,
        long running,
        Instant estimatedReadyAt,
        Backfill backfill
) {

    public record Backfill(Instant since, Integer found, long collected) {
    }
}
