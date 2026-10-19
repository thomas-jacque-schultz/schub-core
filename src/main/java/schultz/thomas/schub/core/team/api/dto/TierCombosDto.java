package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

// Pages publiques par palier (Schub#112). positions dans l'ordre des champions de chaque ligne.
public record TierCombosDto(
        String tier,
        String kind,
        List<String> positions,
        Instant computedAt,
        long games,
        int threshold,
        List<Line> combos
) {

    public record Line(List<Champion> champions, long games, long wins, double winRate, double lowerBound,
                       boolean lowConfidence) {
    }

    public record Champion(int id, String key, String name, String iconUrl) {
    }
}
