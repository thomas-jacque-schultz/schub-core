package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

public record GameReviewDto(
        String id,
        String matchId,
        String subjectMemberId,
        String subjectDisplayName,
        String authorMemberId,
        String authorDisplayName,
        String content,
        Instant createdAt,
        Instant updatedAt,
        boolean viewerCanEdit
) {
}
