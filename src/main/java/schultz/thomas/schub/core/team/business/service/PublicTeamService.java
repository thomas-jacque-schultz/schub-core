package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.team.business.model.PublicTeam;
import schultz.thomas.schub.core.team.business.model.TeamSynergy;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

// Sans acteur : une équipe privée répond comme une équipe inexistante (404), jamais 403.
@Service
@RequiredArgsConstructor
public class PublicTeamService {

    static final int CHAMPIONS = 5;
    static final int MOIS_JOURS = 30;

    private final TeamService teamService;
    private final TeamRepository teams;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final TeamSynergyService synergy;

    public List<PublicTeam.Listed> listed() {
        return teams.findByPublicPageTrue().stream()
                .map(team -> new PublicTeam.Listed(team.getId(), team.getName()))
                .toList();
    }

    public PublicTeam of(String teamId) {
        Team team = teamService.require(teamId);
        if (!team.isPublicPage()) {
            throw new NoSuchElementException("Aucune équipe publique d'identifiant '" + teamId + "'");
        }
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        Map<String, String> publics = new HashMap<>();
        joueurs.forEach(m -> publics.put(m.getMemberId(), nomPublic(m)));

        List<RiotStatsGateway.SharedMatch> parties = parPuuid.size() < TeamGamesStatsService.MINIMUM_MEMBRES
                ? List.of()
                : statsGateway.sharedMatches(List.copyOf(parPuuid.keySet()), TeamGamesStatsService.MINIMUM_MEMBRES,
                                null, TeamGamesStatsService.PARTIES_MAX)
                        .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                        .filter(p -> p.win() != null && !p.splitSides())
                        .toList();
        Instant mois = Instant.now().minus(MOIS_JOURS, ChronoUnit.DAYS);
        List<RiotStatsGateway.SharedMatch> duMois = parties.stream()
                .filter(p -> p.startedAt() != null && p.startedAt().isAfter(mois)).toList();

        return new PublicTeam(team.getId(), team.getName(),
                joueurs.stream().map(m -> {
                    String riotId = publics.get(m.getMemberId());
                    return new PublicTeam.Member(riotId,
                            riotId == null ? null : riotId.replaceFirst("#([^#]*)$", "-$1"), m.getRoles());
                }).toList(),
                parties.size(), victoires(parties), duMois.size(), victoires(duMois),
                meilleurDuo(team.getId(), publics),
                champions(parties));
    }

    private static String nomPublic(TeamMember membre) {
        if (membre.isHiddenOnPublicPage() || membre.getRiotGameName() == null || membre.getRiotTagLine() == null) {
            return null;
        }
        return membre.getRiotGameName() + "#" + membre.getRiotTagLine();
    }

    private static long victoires(List<RiotStatsGateway.SharedMatch> parties) {
        return parties.stream().filter(p -> Boolean.TRUE.equals(p.win())).count();
    }

    // Seulement un duo qui gagne mieux ensemble qu'attendu, et dont les deux noms sont publics.
    private PublicTeam.BestDuo meilleurDuo(String teamId, Map<String, String> publics) {
        return synergy.of(teamId, null).duos().stream()
                .filter(d -> d.delta() != null && d.delta() > 0)
                .filter(d -> publics.get(d.memberA()) != null && publics.get(d.memberB()) != null)
                .max(Comparator.comparingDouble(TeamSynergy.Duo::delta))
                .map(d -> new PublicTeam.BestDuo(publics.get(d.memberA()), publics.get(d.memberB()), d.games(),
                        d.wins(), d.delta()))
                .orElse(null);
    }

    private List<PublicTeam.Champion> champions(List<RiotStatsGateway.SharedMatch> parties) {
        Map<Integer, long[]> comptes = new HashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            boolean victoire = Boolean.TRUE.equals(partie.win());
            partie.players().forEach(j -> {
                long[] c = comptes.computeIfAbsent(j.championId(), k -> new long[2]);
                c[0]++;
                c[1] += victoire ? 1 : 0;
            });
        }
        Map<Integer, RiotChampionGateway.Champion> catalogue = championGateway.catalogue()
                .map(RiotChampionGateway.Catalogue::parId).orElseGet(Map::of);
        return comptes.entrySet().stream()
                .sorted(Map.Entry.<Integer, long[]>comparingByValue(Comparator.comparingLong(c -> c[0])).reversed())
                .limit(CHAMPIONS)
                .map(e -> {
                    RiotChampionGateway.Champion champion = catalogue.get(e.getKey());
                    return new PublicTeam.Champion(e.getKey(), champion == null ? null : champion.name(),
                            champion == null ? null : champion.iconUrl(), e.getValue()[0], e.getValue()[1]);
                })
                .filter(c -> Objects.nonNull(c.name()))
                .toList();
    }
}
