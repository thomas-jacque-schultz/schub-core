package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.team.api.dto.CompanionsDto;
import schultz.thomas.schub.core.team.business.model.PlayerRef;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// Réponse du 05/10 sur #96 : au moins 5 parties ensemble dans le mois en cours. Trois d'entre eux réunis
// cinq fois avec le joueur, c'est déjà un premade : on lui propose de créer l'équipe.
@Service
@RequiredArgsConstructor
public class CompanionsService {

    static final int MINIMUM = 5;
    static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final SearchedPlayerService players;
    private final RiotStatsGateway statsGateway;

    public CompanionsDto of(String slug) {
        PlayerRef joueur = players.resolve(slug);
        YearMonth mois = YearMonth.now(PARIS);
        Instant debut = mois.atDay(1).atStartOfDay(PARIS).toInstant();
        List<RiotStatsGateway.SharedMatch> parties = statsGateway.playerMatches(joueur.puuid(), debut, null)
                .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                .filter(p -> p.win() != null)
                .toList();
        return calcule(joueur.puuid(), mois.toString(), parties);
    }

    static CompanionsDto calcule(String puuid, String mois, List<RiotStatsGateway.SharedMatch> parties) {
        Map<String, long[]> ensemble = new LinkedHashMap<>();
        Map<String, String> riotIds = new HashMap<>();
        List<Set<String>> presences = new ArrayList<>();
        long victoires = 0;
        for (RiotStatsGateway.SharedMatch partie : parties) {
            boolean victoire = Boolean.TRUE.equals(partie.win());
            victoires += victoire ? 1 : 0;
            Set<String> presents = new TreeSet<>();
            VueDePartie.duJoueur(partie, puuid).allies().stream()
                    .filter(o -> o.puuid() != null && !puuid.equals(o.puuid()) && o.riotId() != null)
                    .forEach(o -> {
                        long[] compte = ensemble.computeIfAbsent(o.puuid(), k -> new long[2]);
                        compte[0]++;
                        compte[1] += victoire ? 1 : 0;
                        riotIds.put(o.puuid(), o.riotId());
                        presents.add(o.puuid());
                    });
            presences.add(presents);
        }
        long total = parties.size();
        long totalVictoires = victoires;
        List<CompanionsDto.Companion> compagnons = ensemble.entrySet().stream()
                .filter(e -> e.getValue()[0] >= MINIMUM)
                .map(e -> {
                    long avec = e.getValue()[0];
                    long sans = total - avec;
                    long victoiresSans = totalVictoires - e.getValue()[1];
                    String riotId = riotIds.get(e.getKey());
                    return new CompanionsDto.Companion(riotId, riotId.replaceFirst("#([^#]*)$", "-$1"), avec,
                            e.getValue()[1], (double) e.getValue()[1] / avec, sans,
                            sans >= MINIMUM ? (double) victoiresSans / sans : null);
                })
                .sorted(Comparator.comparingLong(CompanionsDto.Companion::games).reversed())
                .toList();
        return new CompanionsDto(mois, MINIMUM, compagnons, trio(compagnons, riotIds, presences));
    }

    // Le trio de compagnons le plus souvent réuni autour du joueur, s'il l'a été au moins 5 fois.
    private static List<String> trio(List<CompanionsDto.Companion> compagnons, Map<String, String> riotIds,
                                     List<Set<String>> presences) {
        Set<String> retenus = new TreeSet<>();
        riotIds.forEach((puuid, riotId) -> {
            if (compagnons.stream().anyMatch(c -> c.riotId().equals(riotId))) {
                retenus.add(puuid);
            }
        });
        Map<List<String>, Integer> trios = new HashMap<>();
        for (Set<String> presents : presences) {
            List<String> liste = presents.stream().filter(retenus::contains).toList();
            for (int i = 0; i < liste.size(); i++) {
                for (int j = i + 1; j < liste.size(); j++) {
                    for (int k = j + 1; k < liste.size(); k++) {
                        trios.merge(List.of(liste.get(i), liste.get(j), liste.get(k)), 1, Integer::sum);
                    }
                }
            }
        }
        return trios.entrySet().stream()
                .filter(e -> e.getValue() >= MINIMUM)
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().stream().map(riotIds::get).toList())
                .orElse(List.of());
    }
}
