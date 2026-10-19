package schultz.thomas.schub.core.team.business.model;

import schultz.thomas.schub.core.team.business.service.RiotDataGateway.IngestPause;

import java.time.Instant;

public record IngestPauseState(boolean available, boolean paused, Instant updatedAt, long running) {

    public static IngestPauseState from(IngestPause pause) {
        return new IngestPauseState(true, pause.paused(), pause.updatedAt(), pause.running());
    }

    public static IngestPauseState unavailable() {
        return new IngestPauseState(false, false, null, 0);
    }
}
