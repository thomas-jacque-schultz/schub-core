package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

public record StatComparisonDto(
        long referenceGames,
        @Nullable Double winRateDelta,
        @Nullable Double kdaDelta
) {
}
