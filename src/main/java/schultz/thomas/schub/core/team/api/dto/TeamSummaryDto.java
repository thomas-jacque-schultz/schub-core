package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

public record TeamSummaryDto(
        String id,
        String name,
        int memberCount,
        Instant createdAt,
        Instant updatedAt,
        String viewerMemberId,
        boolean viewerCanManage,
        boolean viewerCanEditRoster,
        boolean viewerCanEditCompositions
) {
}
