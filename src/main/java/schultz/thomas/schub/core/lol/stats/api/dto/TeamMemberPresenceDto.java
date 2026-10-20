package schultz.thomas.schub.core.lol.stats.api.dto;

import schultz.thomas.schub.core.lol.stats.business.model.StatsState;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

import jakarta.annotation.Nullable;

public record TeamMemberPresenceDto(
        String memberId,
        @Nullable String displayName,
        MemberStatus status,
        long games,
        long wins,
        @Nullable Double winRate,
        @Nullable Double presenceRate,
        StatsState state
) {
}
