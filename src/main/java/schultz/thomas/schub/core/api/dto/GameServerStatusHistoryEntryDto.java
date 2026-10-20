package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record GameServerStatusHistoryEntryDto(
        @Nullable String status,
        Instant startedAt
) {
}
