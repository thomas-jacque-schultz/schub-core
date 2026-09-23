package schultz.thomas.schub.core.team.api.dto;

public record TeamComparisonDto(
        int comparedWith,
        Double winRateDelta,
        Double kdaDelta,
        Double goldPerMinuteDelta,
        Double damagePerMinuteDelta,
        Double visionPerMinuteDelta
) {
}
