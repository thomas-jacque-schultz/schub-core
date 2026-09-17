package schultz.thomas.schub.core.api.dto;

import java.time.Instant;

public record GameServerStatusHistoryEntryDto(
        String status,
        Instant startedAt
) {
}
