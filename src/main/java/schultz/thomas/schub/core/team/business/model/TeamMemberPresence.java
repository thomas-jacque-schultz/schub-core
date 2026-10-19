package schultz.thomas.schub.core.team.business.model;

import schultz.thomas.schub.core.team.data.model.MemberStatus;

public record TeamMemberPresence(
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
