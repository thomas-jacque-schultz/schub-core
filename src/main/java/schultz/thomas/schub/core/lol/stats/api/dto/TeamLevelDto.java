package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.util.List;

// Chaque partie d'équipe se situe dans les parties du même palier ; on moyenne les percentiles, pas les valeurs.
public record TeamLevelDto(int games, @Nullable String tier, List<String> patches, List<Metric> metrics) {

    @Schema(name = "TeamLevelMetric")
    public record Metric(
            String key,
            @Nullable String polarity,
            int games,
            @Nullable Double mean,
            // De 0 à 1, « plus haut = mieux » quel que soit le sens de la métrique.
            @Nullable Double inTier,
            // Palier dont la moyenne par partie est la plus proche de celle de l'équipe, quand la métrique suit le rang : l'icône.
            @Nullable String level,
            // Fin de partie, sur les parties qui ont aussi la valeur à 15 min ; nulle pour ce qui n'existe qu'en début de partie.
            int gamesAtEnd,
            @Nullable Double meanAtEnd,
            @Nullable Double meanChange
    ) {
    }
}
