package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record ChampionPoolDto(
        String teamId,
        String teamName,
        @Nullable String patch,
        int masteryFloor,
        int teamMasteryFloor,
        List<ChampionCatalogEntryDto> catalog,
        List<ChampionPoolColumnDto> columns,
        @Nullable String viewerMemberId,
        boolean viewerCanEdit,
        boolean startingSelection,
        Instant generatedAt
) {
}
