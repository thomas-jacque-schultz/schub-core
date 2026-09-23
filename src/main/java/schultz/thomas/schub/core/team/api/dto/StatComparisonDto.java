package schultz.thomas.schub.core.team.api.dto;

public record StatComparisonDto(
        long referenceGames,
        Double winRateDelta,
        Double kdaDelta
) {
}
