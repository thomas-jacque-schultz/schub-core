package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

// Poste le plus joué et palier : de quoi lire la bonne grille de /lol/references. met : ses adversaires directs.
public record RadarReferencesDto(String position, @Nullable String tier, @Nullable MetricReferenceDto met) {
}
