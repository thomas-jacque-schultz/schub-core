package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

public record GameServerMemberDto(
        String id,
        String slug,
        String name,
        String urlConnection,
        String game,
        String gameLabel,
        String gameIconUrl,
        Integer playersMax,
        String installation,
        String version,
        String description,
        String status,
        Instant lastStatusCheckAt,
        Instant lastStatusChangeAt,
        List<GameServerStatusHistoryEntryDto> statusHistory
) {
}
