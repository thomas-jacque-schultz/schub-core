package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.PlayerStatsDto;
import schultz.thomas.schub.core.team.api.dto.RankedStandingDto;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;
import schultz.thomas.schub.core.team.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TeamPlayerStatsService {

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final PlayerStatsService playerStatsService;
    private final RiotStatsGateway statsGateway;

    private static final int PARTIES_D_EQUIPE_MAX = 1000;

    public TeamPlayersStatsDto of(User actor, String teamId, Integer days, Integer champions) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = joueursDe(team);
        int championsMax = PlayerStatsService.bornerChampions(champions);
        Instant since = PlayerStatsService.depuis(days);

        List<String> puuids = joueurs.stream()
                .map(TeamMember::getRiotPuuid)
                .filter(puuid -> puuid != null && !puuid.isBlank())
                .distinct()
                .toList();
        Map<String, PlayerStatsService.Figures> figures =
                playerStatsService.of(puuids, since, championsMax);
        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(joueurs);
        Map<String, List<RankedStandingDto>> rangs = Parallele.parCle(figures.entrySet().stream()
                .filter(entree -> entree.getValue().state() == StatsState.STATISTIQUES_CONNUES)
                .map(Map.Entry::getKey)
                .toList(), playerStatsService::rankings);

        Optional<Premade> premade = premade(puuids, since);

        List<PlayerStatsDto> colonnes = new ArrayList<>();
        for (TeamMember membre : joueurs) {
            colonnes.add(colonne(membre, figures, identites, rangs,
                    premade.flatMap(p -> p.ligneDe(membre.getRiotPuuid())).orElse(null)));
        }
        return new TeamPlayersStatsDto(team.getId(), team.getName(), days, championsMax, colonnes,
                placeDuLecteur(team, actor), Instant.now(), premade.map(Premade::parties).orElse(null),
                TeamGamesStatsService.MINIMUM_MEMBRES);
    }

    private record Premade(long parties, Map<String, StatLineDto> lignes) {

        Optional<StatLineDto> ligneDe(String puuid) {
            return puuid == null ? Optional.empty() : Optional.ofNullable(lignes.get(puuid));
        }
    }

    private Optional<Premade> premade(List<String> puuids, Instant since) {
        if (puuids.size() < TeamGamesStatsService.MINIMUM_MEMBRES) {
            return Optional.of(new Premade(0, Map.of()));
        }
        Optional<RiotStatsGateway.SharedMatches> communes = statsGateway.sharedMatches(puuids,
                TeamGamesStatsService.MINIMUM_MEMBRES, since, PARTIES_D_EQUIPE_MAX);
        if (communes.isEmpty()) {
            return Optional.empty();
        }
        List<String> ids = communes.get().matches().stream().map(RiotStatsGateway.SharedMatch::matchId).toList();
        if (ids.isEmpty()) {
            return Optional.of(new Premade(0, Map.of()));
        }
        return statsGateway.aggregate(puuids, RiotStatsGateway.Grouping.OVERALL, RiotStatsGateway.Scope.RIFT,
                        since, ids)
                .map(buckets -> {
                    Map<String, StatLineDto> lignes = new LinkedHashMap<>();
                    buckets.stream().filter(bucket -> bucket.games() > 0)
                            .forEach(bucket -> lignes.put(bucket.puuid(), StatLines.of(bucket, null, null, null)));
                    return new Premade(ids.size(), lignes);
                });
    }

    static List<TeamMember> joueursDe(Team team) {
        if (team.getMembers() == null) {
            return List.of();
        }
        return team.getMembers().stream()
                .filter(membre -> membre.getStatus() != MemberStatus.COACH)
                .sorted(Comparator
                        .comparingInt((TeamMember membre) -> rang(membre.mainRole()))
                        .thenComparing(membre -> membre.getStatus() != MemberStatus.TITULAIRE)
                        .thenComparing(membre -> Optional.ofNullable(membre.riotId()).orElse(""),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static int rang(GameRole role) {
        return role == null ? GameRole.values().length : role.ordinal();
    }

    private PlayerStatsDto colonne(TeamMember membre,
                                   Map<String, PlayerStatsService.Figures> figures,
                                   Map<String, MemberDirectory.MemberIdentity> identites,
                                   Map<String, List<RankedStandingDto>> rangs,
                                   StatLineDto premade) {
        String puuid = membre.getRiotPuuid();
        PlayerStatsService.Figures chiffres =
                puuid == null || puuid.isBlank() ? null : figures.get(puuid);
        MemberDirectory.MemberIdentity identite =
                membre.getUserId() == null ? null : identites.get(membre.getUserId());
        StatsState state = chiffres == null ? StatsState.COMPTE_RIOT_ABSENT : chiffres.state();

        return new PlayerStatsDto(
                membre.getMemberId(),
                nomAffiche(membre, identite),
                identite == null ? null : identite.avatarUrl(),
                membre.getRiotGameName(),
                membre.getRiotTagLine(),
                membre.getStatus(),
                membre.getRoles(),
                membre.isLinked(),
                state,
                chiffres == null ? null : chiffres.coverage(),
                chiffres == null ? null : chiffres.overall(),
                chiffres == null ? List.of() : chiffres.champions(),
                chiffres == null ? List.of() : chiffres.positions(),
                chiffres == null ? List.of() : chiffres.queues(),
                chiffres == null ? List.of() : chiffres.months(),
                state == StatsState.STATISTIQUES_CONNUES ? rangs.getOrDefault(puuid, List.of()) : List.of(),
                chiffres == null ? null : chiffres.references(),
                premade);
    }

    private static String nomAffiche(TeamMember membre, MemberDirectory.MemberIdentity identite) {
        if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
            return identite.displayName();
        }
        return membre.riotId();
    }

    private Map<String, MemberDirectory.MemberIdentity> resoutLesComptes(List<TeamMember> joueurs) {
        Set<String> ids = new HashSet<>();
        joueurs.stream()
                .map(TeamMember::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        return ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
    }

    static String placeDuLecteur(Team team, User actor) {
        if (actor == null || actor.getId() == null || team.getMembers() == null) {
            return null;
        }
        return team.getMembers().stream()
                .filter(membre -> actor.getId().equals(membre.getUserId()))
                .map(TeamMember::getMemberId)
                .findFirst()
                .orElse(null);
    }

    static Map<String, TeamMember> parPuuid(List<TeamMember> joueurs) {
        Map<String, TeamMember> index = new LinkedHashMap<>();
        joueurs.forEach(membre -> {
            String puuid = membre.getRiotPuuid();
            if (puuid != null && !puuid.isBlank()) {
                index.putIfAbsent(puuid, membre);
            }
        });
        return index;
    }
}
