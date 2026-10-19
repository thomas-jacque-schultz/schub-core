package schultz.thomas.schub.core.business.model;

import java.time.Instant;

// backfill : absent si aucun rattrapage n'a été demandé (Schub#113).
public record RiotIngest(
        long pending,
        long running,
        Instant estimatedReadyAt,
        Backfill backfill
) {

    public record Backfill(Instant since, Integer found, long collected) {
    }
}
