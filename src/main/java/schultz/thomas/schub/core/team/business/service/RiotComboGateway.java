package schultz.thomas.schub.core.team.business.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Optional vide : le connecteur ne répond pas, ou n'a pas encore calculé la table du jour.
public interface RiotComboGateway {

    Optional<List<GeneralCombo>> combos(List<Slot> slots, int minGames);

    Optional<TierCombos> tierCombos(String tier, String kind, int minGames, int limit);

    record TierCombos(String tier, String kind, Instant computedAt, long games, List<Ranked> combos) {
    }

    record Ranked(List<Integer> championIds, long games, long wins, double lowerBound) {
    }

    // position : celle de Riot (TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY).
    record Slot(String position, List<Integer> championIds) {
    }

    record GeneralCombo(List<Integer> championIds, long games, long wins) {
    }
}
