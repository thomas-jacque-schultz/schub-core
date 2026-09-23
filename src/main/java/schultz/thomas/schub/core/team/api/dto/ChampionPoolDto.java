package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record ChampionPoolDto(
        String teamId,
        String teamName,
        String patch,
        int masteryFloor,
        int teamMasteryFloor,
        List<ChampionCatalogEntryDto> catalog,
        List<ChampionPoolColumnDto> columns,
        String viewerMemberId,
        boolean viewerCanEdit,
        Instant generatedAt
) {
}
