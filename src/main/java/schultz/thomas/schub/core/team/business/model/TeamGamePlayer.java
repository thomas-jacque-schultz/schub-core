package schultz.thomas.schub.core.team.business.model;

import schultz.thomas.schub.core.team.business.service.RiotStatsGateway;

public record TeamGamePlayer(
        String memberId,
        String displayName,
        String riotId,
        int championId,
        String championName,
        String iconUrl,
        String position,
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
        RankedStanding soloRank,
        RankedStanding flexRank,
        RiotStatsGateway.At15 at15
) {
}
