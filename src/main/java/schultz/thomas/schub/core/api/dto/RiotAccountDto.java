package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.RiotAccountState;

import java.time.Instant;

public record RiotAccountDto(
        RiotAccountState state,
        String riotId,
        String gameName,
        String tagLine,
        Instant linkedAt,
        RiotIngestDto ingest,
        RiotAccountChangeDto change
) {

    public RiotAccountDto withIngest(RiotIngestDto ingest) {
        return new RiotAccountDto(state, riotId, gameName, tagLine, linkedAt, ingest, change);
    }

    public RiotAccountDto withChange(RiotAccountChangeDto change) {
        return new RiotAccountDto(state, riotId, gameName, tagLine, linkedAt, ingest, change);
    }
}
