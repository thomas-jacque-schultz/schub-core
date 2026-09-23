package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;

import java.time.Instant;

public record ChampionPoolMemberDto(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        MemberStatus status,
        boolean linked,
        PoolState state,
        Integer masteryLevel,
        Integer masteryPoints,
        Instant lastPlayedAt,
        Instant observedAt
) {
}
