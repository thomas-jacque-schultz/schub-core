package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record GameReviewsDto(
        String teamId,
        String matchId,
        List<GameReviewDto> reviews,
        String viewerMemberId,
        boolean viewerCanReviewAnyone,
        Instant generatedAt
) {
}
