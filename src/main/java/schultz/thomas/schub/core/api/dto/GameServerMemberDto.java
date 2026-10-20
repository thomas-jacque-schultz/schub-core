package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record GameServerMemberDto(
        @Nullable String id,
        @Nullable String slug,
        @Nullable String name,
        @Nullable String urlConnection,
        @Nullable String game,
        @Nullable String gameLabel,
        @Nullable String gameIconUrl,
        @Nullable Integer playersMax,
        @Nullable String installation,
        @Nullable String version,
        @Nullable String description,
        @Nullable String status,
        @Nullable Instant lastStatusCheckAt,
        @Nullable Instant lastStatusChangeAt,
        @Nullable List<GameServerStatusHistoryEntryDto> statusHistory,
        @Nullable GameServerCommandFailureDto lastCommandFailure,
        @Nullable List<GameServerReferentDto> referents,
        boolean viewerCanControl
) {

    public GameServerMemberDto forViewer(List<GameServerReferentDto> resolvedReferents, boolean canControl) {
        return new GameServerMemberDto(id, slug, name, urlConnection, game, gameLabel, gameIconUrl, playersMax,
                installation, version, description, status, lastStatusCheckAt, lastStatusChangeAt, statusHistory,
                lastCommandFailure, resolvedReferents, canControl);
    }
}
