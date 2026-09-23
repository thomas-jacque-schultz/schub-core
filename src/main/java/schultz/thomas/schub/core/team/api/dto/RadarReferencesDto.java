package schultz.thomas.schub.core.team.api.dto;

// Le poste le plus joué sur la période : c'est à ce poste que le joueur se compare.
public record RadarReferencesDto(String position, String tier, MetricReferenceDto league,
                                 MetricReferenceDto met) {
}
