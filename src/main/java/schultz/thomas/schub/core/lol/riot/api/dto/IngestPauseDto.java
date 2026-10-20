package schultz.thomas.schub.core.lol.riot.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record IngestPauseDto(boolean available, boolean paused, @Nullable Instant updatedAt, long running) {
}
