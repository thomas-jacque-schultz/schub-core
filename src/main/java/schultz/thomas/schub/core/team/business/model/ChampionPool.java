package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;
import java.util.List;

public record ChampionPool(
        String teamId,
        String teamName,
        String patch,
        int masteryFloor,
        int teamMasteryFloor,
        List<ChampionCatalogEntry> catalog,
        List<ChampionPoolColumn> columns,
        String viewerMemberId,
        boolean viewerCanEdit,
        boolean startingSelection,
        Instant generatedAt
) {
}
