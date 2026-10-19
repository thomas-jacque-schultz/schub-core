package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.business.model.PoolState;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

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
        Long games,
        Double winRate,
        Instant lastPlayedAt,
        Instant observedAt
) {
}
