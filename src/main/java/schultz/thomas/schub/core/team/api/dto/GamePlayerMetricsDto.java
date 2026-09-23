package schultz.thomas.schub.core.team.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Un joueur d'une partie : ses indicateurs sur la partie et sa moyenne au même poste.")
public record GamePlayerMetricsDto(
        @Schema(description = "Avec championId, désigne le joueur dans la partie.") int side,
        int championId,
        String position,
        @Schema(description = "Palier tenu à la date de la partie.") String tier,
        boolean tierEstimated,
        @Schema(description = "Clé de métrique → valeur sur la partie, calculée comme la grille GAME.")
        Map<String, Double> game,
        @Schema(description = "Moyenne du joueur à ce poste sur la période, parties collectées ; absente hors Faille.")
        StatLineDto average
) {
}
