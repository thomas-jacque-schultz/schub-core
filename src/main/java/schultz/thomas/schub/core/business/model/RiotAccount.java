package schultz.thomas.schub.core.business.model;

import java.time.Instant;

public record RiotAccount(
        RiotAccountState state,
        String riotId,
        String gameName,
        String tagLine,
        Instant linkedAt,
        RiotIngest ingest,
        RiotAccountChange change
) {

    public RiotAccount withIngest(RiotIngest ingest) {
        return new RiotAccount(state, riotId, gameName, tagLine, linkedAt, ingest, change);
    }

    public RiotAccount withChange(RiotAccountChange change) {
        return new RiotAccount(state, riotId, gameName, tagLine, linkedAt, ingest, change);
    }
}
