package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

public record TeamGamePlayerDto(
        @Nullable String memberId,
        @Nullable String displayName,
        @Nullable String riotId,
        int championId,
        @Nullable String championName,
        @Nullable String iconUrl,
        @Nullable String position,
        int side,
        boolean win,
        int kills,
        int deaths,
        int assists,
        int goldEarned,
        int damageToChampions,
        int damageTaken,
        int minionsKilled,
        int visionScore,
        boolean afk,
        @Nullable RankedStandingDto soloRank,
        @Nullable RankedStandingDto flexRank,
        @Nullable At15Dto at15
) {
}
