package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

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
