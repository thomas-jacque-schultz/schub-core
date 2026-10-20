package schultz.thomas.schub.core.lol.draft.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

// Pages publiques par palier (Schub#112). positions dans l'ordre des champions de chaque ligne.
public record TierCombosDto(
        String tier,
        String kind,
        List<String> positions,
        Instant computedAt,
        long games,
        int minGames,
        List<Line> combos
) {

    public record Line(List<Champion> champions, long games, long wins, double winRate, double lowerBound) {
    }

    @Schema(name = "TierComboChampion")
    public record Champion(int id, @Nullable String key, @Nullable String name, @Nullable String iconUrl) {
    }
}
