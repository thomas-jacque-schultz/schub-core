package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record TeamDto(
        String id,
        String name,
        int memberCount,
        List<TeamMemberDto> members,
        Instant createdAt,
        Instant updatedAt,
        String viewerMemberId,
        boolean viewerCanEdit,
        boolean viewerCanEditCompositions
) {
}
