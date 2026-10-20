package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record TeamSummaryDto(
        String id,
        String name,
        int memberCount,
        Instant createdAt,
        Instant updatedAt,
        @Nullable String viewerMemberId,
        boolean viewerCanManage,
        boolean viewerCanEditRoster,
        boolean viewerCanEditCompositions
) {
}
