package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.service.RiotDataGateway.IngestPause;

import java.time.Instant;

public record IngestPauseDto(boolean available, boolean paused, Instant updatedAt, long running) {

    public static IngestPauseDto from(IngestPause pause) {
        return new IngestPauseDto(true, pause.paused(), pause.updatedAt(), pause.running());
    }

    public static IngestPauseDto unavailable() {
        return new IngestPauseDto(false, false, null, 0);
    }
}
