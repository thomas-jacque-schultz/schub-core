package schultz.thomas.schub.core.lol.stats.api.dto;


public record PositionOppositionDto(
        String position,
        int games,
        Double averageGap,
        TeamRecordDto versusStronger,
        TeamRecordDto versusWeaker,
        int laneGames,
        Double laneWinRate,
        Double averageGoldDiff15,
        Double averageCsDiff15
) {
}
