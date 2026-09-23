package schultz.thomas.schub.core.team.business.service;

import schultz.thomas.schub.core.team.api.dto.AverageRankDto;

import java.util.List;
import java.util.Objects;

// Fer IV = 0, une division = 1, Maître = 28, Challenger = 30 : la même échelle que le radar du front.
final class Rangs {

    static final List<String> PALIERS =
            List.of("IRON", "BRONZE", "SILVER", "GOLD", "PLATINUM", "EMERALD", "DIAMOND");
    static final List<String> APEX = List.of("MASTER", "GRANDMASTER", "CHALLENGER");
    private static final List<String> DIVISIONS = List.of("IV", "III", "II", "I");
    private static final int PREMIER_APEX = PALIERS.size() * DIVISIONS.size();

    private Rangs() {
    }

    static Double valeur(RiotStatsGateway.Standing standing) {
        if (standing == null || standing.tier() == null) {
            return null;
        }
        int apex = APEX.indexOf(standing.tier());
        if (apex >= 0) {
            return (double) PREMIER_APEX + apex;
        }
        int palier = PALIERS.indexOf(standing.tier());
        int division = DIVISIONS.indexOf(standing.division());
        if (palier < 0 || division < 0) {
            return null;
        }
        return palier * DIVISIONS.size() + division + Math.min(standing.leaguePoints(), 99) / 100.0;
    }

    static Double reference(RiotStatsGateway.Standing solo, RiotStatsGateway.Standing flex) {
        Double valeur = valeur(solo);
        return valeur != null ? valeur : valeur(flex);
    }

    static AverageRankDto moyenne(List<RiotStatsGateway.Standing> standings) {
        List<Double> valeurs = standings.stream().map(Rangs::valeur).filter(Objects::nonNull).toList();
        if (valeurs.isEmpty()) {
            return null;
        }
        double moyenne = valeurs.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        return new AverageRankDto(moyenne, palier(moyenne), division(moyenne), valeurs.size());
    }

    static String palier(double valeur) {
        int entier = (int) Math.floor(valeur);
        if (entier >= PREMIER_APEX) {
            return APEX.get(Math.min(APEX.size() - 1, entier - PREMIER_APEX));
        }
        return PALIERS.get(Math.max(0, entier) / DIVISIONS.size());
    }

    private static String division(double valeur) {
        int entier = (int) Math.floor(valeur);
        return entier >= PREMIER_APEX ? null : DIVISIONS.get(Math.max(0, entier) % DIVISIONS.size());
    }
}
