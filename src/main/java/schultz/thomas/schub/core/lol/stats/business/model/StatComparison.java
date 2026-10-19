package schultz.thomas.schub.core.lol.stats.business.model;

public record StatComparison(
        long referenceGames,
        Double winRateDelta,
        Double kdaDelta
) {
}
