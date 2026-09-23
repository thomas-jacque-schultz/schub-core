package schultz.thomas.schub.core.team.api.dto;

public record TeamComparisonDto(
        int comparedWith,
        Double winRateDelta,
        Double kdaDelta,
        Double csPerMinuteDelta,
        Double goldPerMinuteDelta,
        Double damagePerMinuteDelta,
        Double damageTakenPerMinuteDelta,
        Double visionPerMinuteDelta
) {
}
