package schultz.thomas.schub.core.dto;

import java.time.Instant;

public record GameServerStatusHistoryEntryDto(
        String status,
        Instant startedAt
) {
}
