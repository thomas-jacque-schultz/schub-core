package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.StatsState;

public record TeamMemberPresenceDto(
        String memberId,
        String displayName,
        MemberStatus status,
        long games,
        long wins,
        Double winRate,
        Double presenceRate,
        StatsState state
) {
}
