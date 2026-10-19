package schultz.thomas.schub.core.team.business.model;

// Poste le plus joué et palier : de quoi lire la bonne grille de /lol/references. met : ses adversaires directs.
public record RadarReferences(String position, String tier, MetricReference met) {
}
