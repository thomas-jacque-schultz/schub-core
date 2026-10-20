package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.business.model.PoolState;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

import jakarta.annotation.Nullable;
import java.time.Instant;

public record ChampionPoolMemberDto(
        String memberId,
        @Nullable String displayName,
        @Nullable String avatarUrl,
        @Nullable String riotGameName,
        @Nullable String riotTagLine,
        MemberStatus status,
        boolean linked,
        PoolState state,
        @Nullable Integer masteryLevel,
        @Nullable Integer masteryPoints,
        @Nullable Long games,
        @Nullable Double winRate,
        @Nullable Instant lastPlayedAt,
        @Nullable Instant observedAt
) {
}
