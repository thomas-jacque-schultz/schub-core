package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.util.Map;

@Schema(description = "Un joueur d'une partie : ses indicateurs sur la partie et sa moyenne au même poste.")
public record GamePlayerMetricsDto(
        @Schema(description = "Avec championId, désigne le joueur dans la partie.") int side,
        int championId,
        @Nullable String position,
        @Schema(description = "Palier tenu à la date de la partie.") @Nullable String tier,
        boolean tierEstimated,
        @Schema(description = "Clé de métrique → valeur sur la partie, calculée comme la grille GAME.")
        @Nullable Map<String, Double> game,
        @Schema(description = "Moyenne du joueur à ce poste sur la période, parties collectées ; absente hors Faille.")
        @Nullable StatLineDto average
) {
}
