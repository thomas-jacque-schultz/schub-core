package schultz.thomas.schub.core.lol.team.api.dto;

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
        boolean viewerCanManage,
        boolean viewerCanEditRoster,
        boolean viewerCanEditCompositions,
        boolean publicPage,
        List<TrainingSlotDto> trainingSlots
) {
}
