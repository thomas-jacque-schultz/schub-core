package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamMemberPresenceDto;
import schultz.thomas.schub.core.team.api.dto.TeamRecordDto;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TeamGamesStatsService {

    public static final int MINIMUM_MEMBRES = 4;

    public static final int PARTIES_DEFAUT = 100;
    public static final int PARTIES_MAX = 500;
    private static final int PATCHS_RENDUS = 6;

    static final List<String> POSTES = GameViews.POSTES;

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final RiotStatsGateway statsGateway;
    private final GameViews views;

    // Silence du connecteur = RiotConnectorUnavailableException, jamais « non ».
    public boolean estPartieDEquipe(Team team, String matchId) {
        return partieDEquipe(team, matchId).isPresent();
    }

    private Optional<RiotStatsGateway.SharedMatch> partieDEquipe(Team team, String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return Optional.empty();
        }
        Map<String, TeamMember> parPuuid =
                TeamPlayerStatsService.parPuuid(TeamPlayerStatsService.joueursDe(team));
        if (parPuuid.size() < MINIMUM_MEMBRES) {
            return Optional.empty();
        }
        return statsGateway
                .sharedMatchesAmong(List.copyOf(parPuuid.keySet()), MINIMUM_MEMBRES, List.of(matchId))
                .orElseThrow(RiotConnectorUnavailableException::new)
                .matches().stream()
                .filter(partie -> matchId.equals(partie.matchId()))
                .findFirst();
    }

    public TeamGamesStatsDto of(User actor, String teamId, Integer days, Integer limit) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        Instant since = PlayerStatsService.depuis(days);
        int borne = limit == null ? PARTIES_DEFAUT : (int) Math.clamp(limit.longValue(), 1, PARTIES_MAX);

        Map<String, RiotStatsGateway.Coverage> couverture = couverture(parPuuid.keySet());
        Map<String, String> noms = noms(joueurs);

        if (parPuuid.size() < MINIMUM_MEMBRES) {
            return vide(team, actor, joueurs, noms, days, StatsState.EFFECTIF_INCOMPLET, couverture);
        }
        Optional<RiotStatsGateway.SharedMatches> communes = statsGateway.sharedMatches(
                List.copyOf(parPuuid.keySet()), MINIMUM_MEMBRES, since, borne);
        if (communes.isEmpty()) {
            return vide(team, actor, joueurs, noms, days, StatsState.CONNECTEUR_INDISPONIBLE, couverture);
        }
        List<RiotStatsGateway.SharedMatch> parties = communes.get().matches();
        if (parties.isEmpty()) {
            return vide(team, actor, joueurs, noms, days, etatSansPartie(couverture), couverture);
        }

        List<TeamGameDto> rendues = views.parties(parties, parPuuid, noms);
        List<RiotStatsGateway.SharedMatch> decidees = parties.stream()
                .filter(partie -> partie.win() != null)
                .toList();

        return new TeamGamesStatsDto(
                team.getId(),
                team.getName(),
                days,
                MINIMUM_MEMBRES,
                joueurs.size(),
                StatsState.STATISTIQUES_CONNUES,
                bilan("", null, decidees),
                bilans(decidees, TeamGamesStatsService::mode, partie -> null),
                bilans(decidees, partie -> String.valueOf(cote(partie)), partie -> null),
                bilans(decidees, RiotStatsGateway.SharedMatch::patch, partie -> null).stream()
                        .sorted(Comparator.comparing(TeamRecordDto::key, TeamGamesStatsService::parVersion).reversed())
                        .limit(PATCHS_RENDUS)
                        .toList(),
                presences(joueurs, parties, noms, couverture),
                rendues,
                communes.get().totalMatches(),
                parties.size() - decidees.size(),
                communes.get().truncated(),
                parties.stream().map(RiotStatsGateway.SharedMatch::startedAt)
                        .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null),
                parties.stream().map(RiotStatsGateway.SharedMatch::startedAt)
                        .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null),
                TeamPlayerStatsService.placeDuLecteur(team, actor),
                Instant.now());
    }

    public TeamGameDetailDto detail(User actor, String teamId, String matchId, Integer days) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        if (parPuuid.size() < MINIMUM_MEMBRES) {
            throw new NoSuchElementException("Cette équipe n'a pas assez de comptes Riot pour avoir des parties d'équipe");
        }
        RiotStatsGateway.SharedMatch partie = partieDEquipe(team, matchId)
                .orElseThrow(() -> new NoSuchElementException("Aucune partie d'équipe « " + matchId + " »"));
        return views.detail(team.getId(), partie, parPuuid, noms(joueurs),
                TeamPlayerStatsService.placeDuLecteur(team, actor), days);
    }

    private Map<String, RiotStatsGateway.Coverage> couverture(java.util.Collection<String> puuids) {
        if (puuids.isEmpty()) {
            return Map.of();
        }
        Map<String, RiotStatsGateway.Coverage> index = new LinkedHashMap<>();
        statsGateway.coverage(List.copyOf(puuids)).orElseGet(List::of)
                .forEach(row -> index.put(row.puuid(), row));
        return index;
    }

    private TeamGamesStatsDto vide(Team team, User actor, List<TeamMember> joueurs, Map<String, String> noms,
                                   Integer days, StatsState state,
                                   Map<String, RiotStatsGateway.Coverage> couverture) {
        return new TeamGamesStatsDto(team.getId(), team.getName(), days, MINIMUM_MEMBRES,
                joueurs.size(), state, bilan("", null, List.of()), List.of(), List.of(), List.of(),
                presences(joueurs, List.of(), noms, couverture), List.of(), 0, 0,
                false, null, null, TeamPlayerStatsService.placeDuLecteur(team, actor),
                Instant.now());
    }

    private static StatsState etatSansPartie(Map<String, RiotStatsGateway.Coverage> couverture) {
        if (couverture.isEmpty()) {
            return StatsState.AUCUNE_PARTIE;
        }
        boolean rienAnalyse = couverture.values().stream()
                .allMatch(ligne -> ligne.analysedMatches() == 0);
        boolean quelqueChoseEnRoute = couverture.values().stream()
                .anyMatch(ligne -> ligne.tracked() || ligne.knownMatches() > 0);
        return rienAnalyse && quelqueChoseEnRoute ? StatsState.INGESTION_EN_COURS
                : StatsState.AUCUNE_PARTIE;
    }

    private static TeamRecordDto bilan(String key, String label,
                                       List<RiotStatsGateway.SharedMatch> parties) {
        long games = parties.size();
        long wins = parties.stream().filter(partie -> Boolean.TRUE.equals(partie.win())).count();
        long secondes = parties.stream()
                .mapToLong(RiotStatsGateway.SharedMatch::durationSeconds).sum();
        return new TeamRecordDto(key, label, games, wins, games - wins,
                games == 0 ? null : (double) wins / games,
                games == 0 ? null : (double) secondes / games);
    }

    private static List<TeamRecordDto> bilans(
            List<RiotStatsGateway.SharedMatch> parties,
            Function<RiotStatsGateway.SharedMatch, String> cle,
            Function<RiotStatsGateway.SharedMatch, String> libelle) {
        Map<String, List<RiotStatsGateway.SharedMatch>> groupes = new LinkedHashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            String key = Optional.ofNullable(cle.apply(partie)).orElse("");
            groupes.computeIfAbsent(key, ignore -> new ArrayList<>()).add(partie);
        }
        return groupes.entrySet().stream()
                .map(entree -> bilan(entree.getKey(),
                        libelle.apply(entree.getValue().getFirst()), entree.getValue()))
                .sorted(Comparator.comparingLong(TeamRecordDto::games).reversed())
                .toList();
    }

    private static String mode(RiotStatsGateway.SharedMatch partie) {
        return partie.queue() == null || partie.queue().isBlank() ? "OTHER" : partie.queue();
    }

    // « 16.9 » précède « 16.18 » : l'ordre alphabétique les inverserait.
    static int parVersion(String a, String b) {
        int[] x = version(a);
        int[] y = version(b);
        return x[0] != y[0] ? Integer.compare(x[0], y[0]) : Integer.compare(x[1], y[1]);
    }

    private static int[] version(String patch) {
        String[] segments = patch == null ? new String[0] : patch.split("\\.");
        try {
            return new int[] {Integer.parseInt(segments[0]), Integer.parseInt(segments[1])};
        } catch (RuntimeException illisible) {
            return new int[] {-1, -1};
        }
    }

    static int cote(RiotStatsGateway.SharedMatch partie) {
        return GameViews.cote(partie);
    }

    private static List<TeamMemberPresenceDto> presences(
            List<TeamMember> joueurs, List<RiotStatsGateway.SharedMatch> parties,
            Map<String, String> noms, Map<String, RiotStatsGateway.Coverage> couverture) {
        List<TeamMemberPresenceDto> presences = new ArrayList<>();
        for (TeamMember membre : joueurs) {
            String puuid = membre.getRiotPuuid();
            long games = 0;
            long wins = 0;
            for (RiotStatsGateway.SharedMatch partie : parties) {
                Optional<RiotStatsGateway.SharedMatchPlayer> present = partie.players().stream()
                        .filter(joueur -> joueur.puuid().equals(puuid))
                        .findFirst();
                if (present.isPresent()) {
                    games++;
                    if (present.get().win()) {
                        wins++;
                    }
                }
            }
            presences.add(new TeamMemberPresenceDto(
                    membre.getMemberId(),
                    noms.getOrDefault(membre.getMemberId(), membre.riotId()),
                    membre.getStatus(),
                    games,
                    wins,
                    games == 0 ? null : (double) wins / games,
                    parties.isEmpty() ? null : (double) games / parties.size(),
                    etat(puuid, puuid == null ? null : couverture.get(puuid))));
        }
        return presences;
    }

    private static StatsState etat(String puuid, RiotStatsGateway.Coverage couverture) {
        if (puuid == null || puuid.isBlank()) {
            return StatsState.COMPTE_RIOT_ABSENT;
        }
        if (couverture == null) {
            return StatsState.CONNECTEUR_INDISPONIBLE;
        }
        if (couverture.analysedMatches() > 0) {
            return StatsState.STATISTIQUES_CONNUES;
        }
        return couverture.tracked() || couverture.knownMatches() > 0
                ? StatsState.INGESTION_EN_COURS
                : StatsState.AUCUNE_PARTIE;
    }

    Map<String, RiotStatsGateway.Insight> insightsDe(List<RiotStatsGateway.SharedMatch> parties) {
        return views.insights(parties);
    }

    Map<String, String> noms(List<TeamMember> joueurs) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        joueurs.stream()
                .map(TeamMember::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        Map<String, MemberDirectory.MemberIdentity> identites = ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
        Map<String, String> noms = new HashMap<>();
        joueurs.forEach(membre -> noms.put(membre.getMemberId(), nomAffiche(membre,
                membre.getUserId() == null ? null : identites.get(membre.getUserId()))));
        return noms;
    }

    private static String nomAffiche(TeamMember membre, MemberDirectory.MemberIdentity identite) {
        if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
            return identite.displayName();
        }
        return membre.riotId();
    }
}
