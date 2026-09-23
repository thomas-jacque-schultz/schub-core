package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

public record GameServerDto(
        String id,
        String slug,
        Integer deploymentId,
        String name,
        String urlConnection,
        String game,
        String gameLabel,
        String gameIconUrl,
        Integer playersMax,
        String installation,
        String version,
        String description,
        List<GameServerPortDto> ports,
        String status,
        Instant lastStatusCheckAt,
        Instant lastStatusChangeAt,
        List<GameServerStatusHistoryEntryDto> statusHistory
) {
}
