package schultz.thomas.schub.core.team.api.dto;

public record TeamGamePlayerDto(
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
        RankedStandingDto soloRank,
        RankedStandingDto flexRank,
        At15Dto at15
) {
}
