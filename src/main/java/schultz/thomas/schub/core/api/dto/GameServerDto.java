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
        List<GameServerStatusHistoryEntryDto> statusHistory,
        GameServerCommandFailureDto lastCommandFailure,
        List<String> referentIds,
        List<GameServerReferentDto> referents,
        boolean viewerCanControl
) {

    public GameServerDto forViewer(List<GameServerReferentDto> resolvedReferents, boolean canControl) {
        return new GameServerDto(id, slug, deploymentId, name, urlConnection, game, gameLabel, gameIconUrl,
                playersMax, installation, version, description, ports, status, lastStatusCheckAt,
                lastStatusChangeAt, statusHistory, lastCommandFailure, referentIds, resolvedReferents, canControl);
    }
}
