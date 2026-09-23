package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.PositionOppositionDto;
import schultz.thomas.schub.core.team.api.dto.TeamEarlyGameDto;
import schultz.thomas.schub.core.team.api.dto.TeamLevelDto;
import schultz.thomas.schub.core.team.api.dto.TeamOppositionDto;
import schultz.thomas.schub.core.team.api.dto.TeamRecordDto;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Le niveau adverse : jusqu'où l'équipe gagne. Tout repose sur les rangs relevés à la collecte —
 * pour une partie ancienne, un rang d'aujourd'hui ; {@code medianLagDays} dit ce que vaut le relevé.
 */
@Service
@RequiredArgsConstructor
public class TeamOppositionService {

    static final int PLAFOND_MINIMUM = 5;
    // Écart en divisions, nous moins eux. Au-delà de deux divisions, le match-making a déjà beaucoup cédé.
    static final List<String> ECARTS = List.of("MUCH_STRONGER", "STRONGER", "EVEN", "WEAKER", "MUCH_WEAKER");

    private final TeamService teamService;
    private final RiotStatsGateway statsGateway;
    private final TeamGamesStatsService gamesStats;

    public TeamOppositionDto of(User actor, String teamId, Integer days) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        if (parPuuid.size() < TeamGamesStatsService.MINIMUM_MEMBRES) {
            return vide(team, days, StatsState.EFFECTIF_INCOMPLET);
        }
        Optional<RiotStatsGateway.SharedMatches> communes = statsGateway.sharedMatches(
                List.copyOf(parPuuid.keySet()), TeamGamesStatsService.MINIMUM_MEMBRES,
                PlayerStatsService.depuis(days), TeamGamesStatsService.PARTIES_MAX);
        if (communes.isEmpty()) {
            return vide(team, days, StatsState.CONNECTEUR_INDISPONIBLE);
        }
        List<RiotStatsGateway.SharedMatch> decidees = communes.get().matches().stream()
                .filter(partie -> partie.win() != null && !partie.splitSides())
                .toList();
        if (decidees.isEmpty()) {
            return vide(team, days, StatsState.AUCUNE_PARTIE);
        }
        Map<String, RiotStatsGateway.Insight> insights = gamesStats.insightsDe(decidees);
        return calcule(team.getId(), days, decidees, insights, parPuuid.keySet(),
                EarlyGames.bilan(decidees, insights, parPuuid, gamesStats.noms(joueurs)),
                TeamLevels.of(decidees, insights, statsGateway.teamGrid().orElse(null)));
    }

    static TeamOppositionDto calcule(String teamId, Integer days, List<RiotStatsGateway.SharedMatch> parties,
                                     Map<String, RiotStatsGateway.Insight> insights, Set<String> membres,
                                     TeamEarlyGameDto early, TeamLevelDto level) {
        Map<String, List<RiotStatsGateway.SharedMatch>> parPalier = new LinkedHashMap<>();
        Map<String, List<RiotStatsGateway.SharedMatch>> parEcart = new LinkedHashMap<>();
        Map<String, Poste> parPoste = new LinkedHashMap<>();
        TeamGamesStatsService.POSTES.forEach(poste -> parPoste.put(poste, new Poste()));
        List<Double> retards = new ArrayList<>();
        int avecRangs = 0;

        for (RiotStatsGateway.SharedMatch partie : parties) {
            RiotStatsGateway.Insight insight = insights.get(partie.matchId());
            if (insight == null || insight.ranksObservedAt() == null) {
                continue;
            }
            int notreCote = TeamGamesStatsService.cote(partie);
            Double nous = moyenne(insight, true, notreCote);
            Double eux = moyenne(insight, false, notreCote);
            if (eux == null) {
                continue;
            }
            avecRangs++;
            if (partie.startedAt() != null) {
                retards.add(Duration.between(partie.startedAt(), insight.ranksObservedAt()).toHours() / 24.0);
            }
            parPalier.computeIfAbsent(Rangs.palier(eux), cle -> new ArrayList<>()).add(partie);
            if (nous != null) {
                parEcart.computeIfAbsent(ecart(nous - eux), cle -> new ArrayList<>()).add(partie);
            }
            for (RiotStatsGateway.InsightPlayer joueur : insight.participants()) {
                if (!membres.contains(joueur.puuid()) || !parPoste.containsKey(joueur.position())) {
                    continue;
                }
                insight.participants().stream()
                        .filter(adverse -> adverse.side() != joueur.side()
                                && Objects.equals(adverse.position(), joueur.position()))
                        .findFirst()
                        .ifPresent(adverse -> parPoste.get(joueur.position()).ajoute(joueur, adverse, partie));
            }
        }

        List<TeamRecordDto> paliers = parPalier.entrySet().stream()
                .map(entree -> bilan(entree.getKey(), entree.getValue()))
                .sorted(Comparator.comparingInt(TeamOppositionService::ordre))
                .toList();
        String plafond = paliers.stream()
                .filter(bilan -> bilan.games() >= PLAFOND_MINIMUM && bilan.winRate() != null && bilan.winRate() >= 0.5)
                .max(Comparator.comparingInt(TeamOppositionService::ordre))
                .map(TeamRecordDto::key)
                .orElse(null);

        return new TeamOppositionDto(
                teamId, days,
                avecRangs == 0 ? StatsState.INGESTION_EN_COURS : StatsState.STATISTIQUES_CONNUES,
                parties.size(), avecRangs, mediane(retards),
                paliers,
                ECARTS.stream().filter(parEcart::containsKey).map(cle -> bilan(cle, parEcart.get(cle))).toList(),
                parPoste.entrySet().stream().map(entree -> entree.getValue().dto(entree.getKey())).toList(),
                plafond, PLAFOND_MINIMUM, early, level, Instant.now());
    }

    static Double moyenne(RiotStatsGateway.Insight insight, boolean notreCamp, int notreCote) {
        return insight.participants().stream()
                .filter(joueur -> (joueur.side() == notreCote) == notreCamp)
                .map(joueur -> Rangs.reference(joueur.solo(), joueur.flex()))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }

    static String ecart(double divisions) {
        if (divisions <= -4) {
            return "MUCH_STRONGER";
        }
        if (divisions <= -2) {
            return "STRONGER";
        }
        if (divisions < 2) {
            return "EVEN";
        }
        return divisions < 4 ? "WEAKER" : "MUCH_WEAKER";
    }

    private static int ordre(TeamRecordDto bilan) {
        int apex = Rangs.APEX.indexOf(bilan.key());
        return apex >= 0 ? Rangs.PALIERS.size() + apex : Rangs.PALIERS.indexOf(bilan.key());
    }

    private static TeamRecordDto bilan(String key, List<RiotStatsGateway.SharedMatch> parties) {
        long wins = parties.stream().filter(partie -> Boolean.TRUE.equals(partie.win())).count();
        long secondes = parties.stream().mapToLong(RiotStatsGateway.SharedMatch::durationSeconds).sum();
        return new TeamRecordDto(key, null, parties.size(), wins, parties.size() - wins,
                parties.isEmpty() ? null : (double) wins / parties.size(),
                parties.isEmpty() ? null : (double) secondes / parties.size());
    }

    private static Double mediane(List<Double> valeurs) {
        if (valeurs.isEmpty()) {
            return null;
        }
        List<Double> tries = valeurs.stream().sorted().toList();
        int milieu = tries.size() / 2;
        return tries.size() % 2 == 1 ? tries.get(milieu) : (tries.get(milieu - 1) + tries.get(milieu)) / 2;
    }

    private TeamOppositionDto vide(Team team, Integer days, StatsState state) {
        return new TeamOppositionDto(team.getId(), days, state, 0, 0, null, List.of(), List.of(), List.of(),
                null, PLAFOND_MINIMUM, null, null, Instant.now());
    }

    private static final class Poste {
        private final List<Double> ecarts = new ArrayList<>();
        private final List<RiotStatsGateway.SharedMatch> plusForts = new ArrayList<>();
        private final List<RiotStatsGateway.SharedMatch> plusFaibles = new ArrayList<>();
        private int couloirs;
        private int couloirsGagnes;
        private long orEcart;
        private long csEcart;

        void ajoute(RiotStatsGateway.InsightPlayer joueur, RiotStatsGateway.InsightPlayer adverse,
                    RiotStatsGateway.SharedMatch partie) {
            Double nous = Rangs.reference(joueur.solo(), joueur.flex());
            Double eux = Rangs.reference(adverse.solo(), adverse.flex());
            if (nous != null && eux != null) {
                ecarts.add(nous - eux);
                (nous < eux ? plusForts : plusFaibles).add(partie);
            }
            if (joueur.at15() != null && adverse.at15() != null) {
                couloirs++;
                int or = joueur.at15().gold() - adverse.at15().gold();
                orEcart += or;
                csEcart += joueur.at15().cs() - adverse.at15().cs();
                if (or > 0) {
                    couloirsGagnes++;
                }
            }
        }

        PositionOppositionDto dto(String poste) {
            return new PositionOppositionDto(poste, ecarts.size(),
                    ecarts.isEmpty() ? null : ecarts.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
                    bilan("STRONGER", plusForts), bilan("WEAKER", plusFaibles),
                    couloirs,
                    couloirs == 0 ? null : (double) couloirsGagnes / couloirs,
                    couloirs == 0 ? null : (double) orEcart / couloirs,
                    couloirs == 0 ? null : (double) csEcart / couloirs);
        }
    }
}
