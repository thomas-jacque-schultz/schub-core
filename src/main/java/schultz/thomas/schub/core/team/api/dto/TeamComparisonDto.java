package schultz.thomas.schub.core.team.api.dto;

/**
 * L'écart d'un joueur à la moyenne de ses coéquipiers qui ont des données.
 *
 * @param comparedWith nombre de coéquipiers dans la moyenne. À zéro, tous les écarts sont nuls :
 *                     il n'y a personne à qui se comparer, et zéro dirait « pareil qu'eux ».
 */
public record TeamComparisonDto(
        int comparedWith,
        Double winRateDelta,
        Double kdaDelta,
        Double goldPerMinuteDelta,
        Double damagePerMinuteDelta,
        Double visionPerMinuteDelta
) {
}
