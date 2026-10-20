package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

public record PositionOppositionDto(
        String position,
        int games,
        @Nullable Double averageGap,
        TeamRecordDto versusStronger,
        TeamRecordDto versusWeaker,
        int laneGames,
        @Nullable Double laneWinRate,
        @Nullable Double averageGoldDiff15,
        @Nullable Double averageCsDiff15
) {
}
