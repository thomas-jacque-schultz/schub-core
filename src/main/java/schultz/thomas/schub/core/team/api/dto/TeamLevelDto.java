package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

// Chaque partie d'équipe se situe dans les parties du même palier ; on moyenne les percentiles, pas les valeurs.
public record TeamLevelDto(int games, String tier, List<String> patches, List<Metric> metrics) {

    public record Metric(
            String key,
            String polarity,
            int games,
            Double mean,
            // De 0 à 1, « plus haut = mieux » quel que soit le sens de la métrique.
            Double inTier,
            Double ladder,
            // Palier de l'échelle du ladder : l'icône.
            String level
    ) {
    }
}
