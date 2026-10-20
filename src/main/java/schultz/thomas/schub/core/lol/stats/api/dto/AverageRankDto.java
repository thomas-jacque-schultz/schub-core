package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

// Moyenne des joueurs classés seulement : un non-classé n'est pas un Fer IV.
public record AverageRankDto(double value, String tier, @Nullable String division, int counted) {
}
