package schultz.thomas.schub.core.lol.stats.api.dto;



// Poste le plus joué et palier : de quoi lire la bonne grille de /lol/references. met : ses adversaires directs.
public record RadarReferencesDto(String position, String tier, MetricReferenceDto met) {
}
