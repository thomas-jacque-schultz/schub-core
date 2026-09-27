package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.TeamSynergyDto;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamSynergyService {

    // Décision du 27/09 (Schub#24) : dix parties ensemble avant d'afficher une paire.
    static final int DUO_MINIMUM = 10;
    // Sous ce nombre de parties sans le partenaire, l'attendu n'a pas de sens.
    static final int ATTENDU_MINIMUM = 3;
    // Les parties hors Faille (ARAM, Arène) n'ont pas de poste : elles n'entrent pas dans la répartition.
    static final Set<String> POSTES = Set.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

    private final TeamService teamService;
    private final RiotStatsGateway statsGateway;
    private final TeamGamesStatsService gamesStats;

    public TeamSynergyDto of(User actor, String teamId, Integer days) {
        return of(teamService.requireVisible(actor, teamId), days);
    }

    // Sans contrôle d'accès : pour les capteurs du moteur, appelés derrière une route qui l'a fait.
    public TeamSynergyDto of(String teamId, Integer days) {
        return of(teamService.require(teamId), days);
    }

    private TeamSynergyDto of(Team team, Integer days) {
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        if (parPuuid.size() < TeamGamesStatsService.MINIMUM_MEMBRES) {
            return new TeamSynergyDto(0, DUO_MINIMUM, List.of(), List.of());
        }
        List<RiotStatsGateway.SharedMatch> parties = statsGateway.sharedMatches(List.copyOf(parPuuid.keySet()),
                        TeamGamesStatsService.MINIMUM_MEMBRES, PlayerStatsService.depuis(days),
                        TeamGamesStatsService.PARTIES_MAX)
                .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                .filter(p -> p.win() != null && !p.splitSides())
                .toList();
        return new TeamSynergyDto(parties.size(), DUO_MINIMUM,
                duos(parties, parPuuid, gamesStats.noms(joueurs)), ressources(parties));
    }

    static List<TeamSynergyDto.Duo> duos(List<RiotStatsGateway.SharedMatch> parties,
                                         Map<String, TeamMember> parPuuid, Map<String, String> noms) {
        List<String> puuids = new ArrayList<>(parPuuid.keySet());
        List<TeamSynergyDto.Duo> duos = new ArrayList<>();
        for (int i = 0; i < puuids.size(); i++) {
            for (int j = i + 1; j < puuids.size(); j++) {
                String a = puuids.get(i);
                String b = puuids.get(j);
                long[] ensemble = new long[2];
                long[] aSeul = new long[2];
                long[] bSeul = new long[2];
                for (RiotStatsGateway.SharedMatch partie : parties) {
                    Set<String> presents = partie.players().stream()
                            .map(RiotStatsGateway.SharedMatchPlayer::puuid).collect(Collectors.toSet());
                    boolean avecA = presents.contains(a);
                    boolean avecB = presents.contains(b);
                    long gagne = Boolean.TRUE.equals(partie.win()) ? 1 : 0;
                    if (avecA && avecB) {
                        ensemble[0]++;
                        ensemble[1] += gagne;
                    } else if (avecA) {
                        aSeul[0]++;
                        aSeul[1] += gagne;
                    } else if (avecB) {
                        bSeul[0]++;
                        bSeul[1] += gagne;
                    }
                }
                if (ensemble[0] < DUO_MINIMUM) {
                    continue;
                }
                Double taux = (double) ensemble[1] / ensemble[0];
                Double attendu = aSeul[0] >= ATTENDU_MINIMUM && bSeul[0] >= ATTENDU_MINIMUM
                        ? ((double) aSeul[1] / aSeul[0] + (double) bSeul[1] / bSeul[0]) / 2
                        : null;
                TeamMember ma = parPuuid.get(a);
                TeamMember mb = parPuuid.get(b);
                duos.add(new TeamSynergyDto.Duo(ma.getMemberId(), noms.get(ma.getMemberId()), mb.getMemberId(),
                        noms.get(mb.getMemberId()), ensemble[0], ensemble[1], taux, attendu,
                        attendu == null ? null : taux - attendu));
            }
        }
        duos.sort(Comparator.comparing((TeamSynergyDto.Duo d) -> d.delta() == null ? Double.NEGATIVE_INFINITY : d.delta())
                .reversed());
        return duos;
    }

    private record Part(String position, double or, double degats, boolean victoire) {
    }

    static List<TeamSynergyDto.Resource> ressources(List<RiotStatsGateway.SharedMatch> parties) {
        Map<String, List<Part>> parPoste = new LinkedHashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            if (partie.players().isEmpty()) {
                continue;
            }
            int camp = partie.players().get(0).side();
            List<RiotStatsGateway.SharedMatchPlayer> equipe = new ArrayList<>(partie.players());
            partie.others().stream().filter(o -> o.side() == camp).forEach(equipe::add);
            double orTotal = equipe.stream().mapToDouble(RiotStatsGateway.SharedMatchPlayer::goldEarned).sum();
            double degatsTotal = equipe.stream().mapToDouble(RiotStatsGateway.SharedMatchPlayer::damageToChampions).sum();
            if (orTotal <= 0 || degatsTotal <= 0) {
                continue;
            }
            for (RiotStatsGateway.SharedMatchPlayer joueur : partie.players()) {
                if (!POSTES.contains(joueur.position())) {
                    continue;
                }
                parPoste.computeIfAbsent(joueur.position(), p -> new ArrayList<>()).add(new Part(joueur.position(),
                        joueur.goldEarned() / orTotal, joueur.damageToChampions() / degatsTotal,
                        Boolean.TRUE.equals(partie.win())));
            }
        }
        List<TeamSynergyDto.Resource> lignes = new ArrayList<>();
        parPoste.forEach((poste, parts) -> {
            List<Double> ors = parts.stream().map(Part::or).sorted().toList();
            double mediane = ors.get(ors.size() / 2);
            List<Part> dessus = parts.stream().filter(p -> p.or() > mediane).toList();
            List<Part> dessous = parts.stream().filter(p -> p.or() <= mediane).toList();
            lignes.add(new TeamSynergyDto.Resource(poste, parts.size(),
                    moyenne(parts, true, Part::or), moyenne(parts, false, Part::or),
                    moyenne(parts, true, Part::degats), moyenne(parts, false, Part::degats),
                    mediane, dessus.size(), taux(dessus), dessous.size(), taux(dessous),
                    parts.stream().mapToDouble(p -> p.degats() - p.or()).average().orElse(0)));
        });
        return lignes;
    }

    private static Double moyenne(List<Part> parts, boolean victoires,
                                  java.util.function.ToDoubleFunction<Part> valeur) {
        return parts.stream().filter(p -> p.victoire() == victoires).mapToDouble(valeur).average().stream()
                .boxed().findFirst().orElse(null);
    }

    private static Double taux(List<Part> parts) {
        return parts.isEmpty() ? null : parts.stream().filter(Part::victoire).count() / (double) parts.size();
    }
}
