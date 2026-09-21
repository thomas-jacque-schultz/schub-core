package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.service.RiotConnectorService.IngestLoad;

import java.time.Duration;
import java.time.Instant;

public record IngestLoadDto(
        boolean available,
        long pending,
        long running,
        long failed,
        double callsPerMinute,
        Duration estimatedDrain,
        Instant estimatedReadyAt,
        Duration throttledFor) {

    public static IngestLoadDto from(IngestLoad load) {
        return new IngestLoadDto(true, load.pending(), load.running(), load.failed(),
                load.callsPerMinute(), load.estimatedDrain(), load.estimatedReadyAt(),
                load.throttledFor());
    }

    /** Connecteur muet : on le dit, on ne rend pas des zéros qui se liraient comme « rien à faire ». */
    public static IngestLoadDto unavailable() {
        return new IngestLoadDto(false, 0, 0, 0, 0, null, null, null);
    }
}
