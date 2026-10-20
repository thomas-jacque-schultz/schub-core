package schultz.thomas.schub.core.lol.session.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TeamSessionDto(
        String id,
        String teamId,
        LocalDate day,
        Instant startedAt,
        Instant endedAt,
        List<String> matchIds,
        List<String> memberIds,
        long games,
        long wins,
        boolean planned,
        @Nullable String slotId,
        boolean missed,
        Instant detectedAt
) {
}
