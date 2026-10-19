package schultz.thomas.schub.core.lol.riot.api.dto;

import java.time.Instant;

public record IngestPauseDto(boolean available, boolean paused, Instant updatedAt, long running) {
}
