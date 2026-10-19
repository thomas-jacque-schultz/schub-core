package schultz.thomas.schub.core.team.business.model;

public record StatComparison(
        long referenceGames,
        Double winRateDelta,
        Double kdaDelta
) {
}
