package schultz.thomas.schub.core.team.business.model;

public record PositionOpposition(
        String position,
        int games,
        Double averageGap,
        TeamRecord versusStronger,
        TeamRecord versusWeaker,
        int laneGames,
        Double laneWinRate,
        Double averageGoldDiff15,
        Double averageCsDiff15
) {
}
