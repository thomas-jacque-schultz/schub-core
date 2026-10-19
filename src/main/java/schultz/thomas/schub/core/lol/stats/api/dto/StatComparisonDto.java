package schultz.thomas.schub.core.lol.stats.api.dto;

public record StatComparisonDto(
        long referenceGames,
        Double winRateDelta,
        Double kdaDelta
) {
}
