package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record GameServerDto(
        @Nullable String id,
        @Nullable String slug,
        @Nullable Integer deploymentId,
        @Nullable String name,
        @Nullable String urlConnection,
        @Nullable String game,
        @Nullable String gameLabel,
        @Nullable String gameIconUrl,
        @Nullable Integer playersMax,
        @Nullable String installation,
        @Nullable String version,
        @Nullable String description,
        @Nullable List<GameServerPortDto> ports,
        @Nullable String status,
        @Nullable Instant lastStatusCheckAt,
        @Nullable Instant lastStatusChangeAt,
        @Nullable List<GameServerStatusHistoryEntryDto> statusHistory,
        @Nullable GameServerCommandFailureDto lastCommandFailure,
        @Nullable List<String> referentIds,
        @Nullable List<GameServerReferentDto> referents,
        boolean viewerCanControl
) {

    public GameServerDto forViewer(List<GameServerReferentDto> resolvedReferents, boolean canControl) {
        return new GameServerDto(id, slug, deploymentId, name, urlConnection, game, gameLabel, gameIconUrl,
                playersMax, installation, version, description, ports, status, lastStatusCheckAt,
                lastStatusChangeAt, statusHistory, lastCommandFailure, referentIds, resolvedReferents, canControl);
    }
}
