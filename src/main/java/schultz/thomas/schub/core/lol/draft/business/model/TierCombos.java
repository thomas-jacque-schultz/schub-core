package schultz.thomas.schub.core.lol.draft.business.model;

import java.time.Instant;
import java.util.List;

// Pages publiques par palier (Schub#112). positions dans l'ordre des champions de chaque ligne.
public record TierCombos(
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

    public record Champion(int id, String key, String name, String iconUrl) {
    }
}
