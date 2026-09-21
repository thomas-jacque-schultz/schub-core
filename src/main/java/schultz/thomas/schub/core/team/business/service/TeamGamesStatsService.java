package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.TeamGameDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamePlayerDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamMemberPresenceDto;
import schultz.thomas.schub.core.team.api.dto.TeamRecordDto;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * <strong>Le panneau 2 — les parties d'équipe</strong> (plan §D.9).
 *
 * <p>Une partie d'équipe est une partie où <strong>au moins quatre</strong> des membres de
 * l'effectif étaient présents, <strong>toutes files confondues</strong>. Le {@code queueId} est
 * conservé et affiché — une victoire en normale draft ne vaut pas une victoire en flex — mais il
 * ne filtre rien : le critère est la présence des joueurs, pas la file.</p>
 *
 * <p>Le seuil vit ici et nulle part ailleurs : le connecteur compte des présences sans savoir ce
 * qu'est une équipe.</p>
 */
@Service
@RequiredArgsConstructor
public class TeamGamesStatsService {

    /** Quatre, pas trois. Le seuil définit ce qu'est une partie d'équipe et commande tout le panneau. */
    public static final int MINIMUM_MEMBRES = 4;

    public static final int PARTIES_DEFAUT = 100;
    public static final int PARTIES_MAX = 500;
    private static final int PATCHS_RENDUS = 6;

    /** Le maximum que le connecteur rende sur une requête de parties communes. */
    private static final int PARTIES_VERIFIEES = 1000;

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;

    /**
     * Cette partie est-elle une partie de cette équipe ?
     *
     * <p>La question n'a qu'une source : le connecteur, seul à détenir les participations. Son
     * silence lève {@link RiotConnectorUnavailableException} au lieu de répondre « non » — une
     * note refusée se réessaie, une note acceptée sur une partie inconnue ne se rattrape pas.</p>
     *
     * <p>La vérification porte sur les {@value #PARTIES_VERIFIEES} parties d'équipe les plus
     * récentes, soit le maximum que le connecteur rende et le double de ce que le panneau
     * affiche.</p>
     */
    public boolean estPartieDEquipe(Team team, String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return false;
        }
        Map<String, TeamMember> parPuuid =
                TeamPlayerStatsService.parPuuid(TeamPlayerStatsService.joueursDe(team));
        if (parPuuid.size() < MINIMUM_MEMBRES) {
            return false;
        }
        RiotStatsGateway.SharedMatches communes = statsGateway
                .sharedMatches(List.copyOf(parPuuid.keySet()), MINIMUM_MEMBRES, null,
                        PARTIES_VERIFIEES)
                .orElseThrow(RiotConnectorUnavailableException::new);
        return communes.matches().stream()
                .anyMatch(partie -> matchId.equals(partie.matchId()));
    }

    public TeamGamesStatsDto of(User actor, String teamId, Integer days, Integer limit) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        Instant since = PlayerStatsService.depuis(days);
        int borne = limit == null ? PARTIES_DEFAUT : (int) Math.clamp(limit.longValue(), 1, PARTIES_MAX);

        Map<String, RiotStatsGateway.Coverage> couverture = couverture(parPuuid.keySet());

        if (parPuuid.size() < MINIMUM_MEMBRES) {
            return vide(team, actor, joueurs, days, StatsState.EFFECTIF_INCOMPLET, couverture);
        }
        Optional<RiotStatsGateway.SharedMatches> communes = statsGateway.sharedMatches(
                List.copyOf(parPuuid.keySet()), MINIMUM_MEMBRES, since, borne);
        if (communes.isEmpty()) {
            return vide(team, actor, joueurs, days, StatsState.CONNECTEUR_INDISPONIBLE, couverture);
        }
        List<RiotStatsGateway.SharedMatch> parties = communes.get().matches();
        if (parties.isEmpty()) {
            return vide(team, actor, joueurs, days, etatSansPartie(couverture), couverture);
        }

        Optional<RiotChampionGateway.Catalogue> catalogue = championGateway.catalogue();
        Map<String, MemberDirectory.MemberIdentity> identites = identites(joueurs);
        List<TeamGameDto> rendues = parties.stream()
                .map(partie -> toGame(partie, parPuuid, identites, catalogue))
                .toList();

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
                bilans(decidees, partie -> String.valueOf(partie.queueId()),
                        RiotStatsGateway.SharedMatch::queue),
                bilans(decidees, partie -> String.valueOf(cote(partie)), partie -> null),
                bilans(decidees, RiotStatsGateway.SharedMatch::patch, partie -> null).stream()
                        .sorted(Comparator.comparing(TeamRecordDto::key).reversed())
                        .limit(PATCHS_RENDUS)
                        .toList(),
                presences(joueurs, parties, identites, couverture),
                rendues,
                communes.get().totalMatches(),
                parties.size() - decidees.size(),
                communes.get().truncated(),
                parties.stream().map(RiotStatsGateway.SharedMatch::startedAt)
                        .filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(null),
                parties.stream().map(RiotStatsGateway.SharedMatch::startedAt)
                        .filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null),
                TeamPlayerStatsService.placeDuLecteur(team, actor),
                Instant.now());
    }

    // --- interne ---

    private Map<String, RiotStatsGateway.Coverage> couverture(java.util.Collection<String> puuids) {
        if (puuids.isEmpty()) {
            return Map.of();
        }
        Map<String, RiotStatsGateway.Coverage> index = new LinkedHashMap<>();
        statsGateway.coverage(List.copyOf(puuids)).orElseGet(List::of)
                .forEach(row -> index.put(row.puuid(), row));
        return index;
    }

    private TeamGamesStatsDto vide(Team team, User actor, List<TeamMember> joueurs, Integer days,
                                   StatsState state,
                                   Map<String, RiotStatsGateway.Coverage> couverture) {
        return new TeamGamesStatsDto(team.getId(), team.getName(), days, MINIMUM_MEMBRES,
                joueurs.size(), state, bilan("", null, List.of()), List.of(), List.of(), List.of(),
                presences(joueurs, List.of(), identites(joueurs), couverture), List.of(), 0, 0,
                false, null, null, TeamPlayerStatsService.placeDuLecteur(team, actor),
                Instant.now());
    }

    /**
     * Sans partie commune, la question est « pourquoi ». Un effectif dont rien n'est encore
     * analysé attend l'ingestion ; un effectif entièrement collecté qui n'a aucune partie à
     * quatre n'en a réellement aucune, et c'est une réponse, pas une panne.
     */
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

    /** Le côté de l'équipe dans cette partie : celui de ses membres, qui y sont tous ensemble. */
    private static int cote(RiotStatsGateway.SharedMatch partie) {
        return partie.players().isEmpty() ? 0 : partie.players().getFirst().side();
    }

    private static List<TeamMemberPresenceDto> presences(
            List<TeamMember> joueurs, List<RiotStatsGateway.SharedMatch> parties,
            Map<String, MemberDirectory.MemberIdentity> identites,
            Map<String, RiotStatsGateway.Coverage> couverture) {
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
                    nomAffiche(membre, identite(identites, membre)),
                    membre.getStatus(),
                    games,
                    wins,
                    games == 0 ? null : (double) wins / games,
                    parties.isEmpty() ? null : (double) games / parties.size(),
                    etat(puuid, couverture.get(puuid))));
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

    private TeamGameDto toGame(RiotStatsGateway.SharedMatch partie,
                               Map<String, TeamMember> parPuuid,
                               Map<String, MemberDirectory.MemberIdentity> identites,
                               Optional<RiotChampionGateway.Catalogue> catalogue) {
        List<TeamGamePlayerDto> joueurs = partie.players().stream()
                .map(joueur -> {
                    TeamMember membre = parPuuid.get(joueur.puuid());
                    RiotChampionGateway.Champion champion = catalogue
                            .map(cat -> cat.parId().get(joueur.championId()))
                            .orElse(null);
                    return new TeamGamePlayerDto(
                            membre == null ? null : membre.getMemberId(),
                            membre == null ? null : nomAffiche(membre, identite(identites, membre)),
                            joueur.championId(),
                            champion != null ? champion.name() : joueur.championName(),
                            champion == null ? null : champion.iconUrl(),
                            joueur.position(),
                            joueur.side(),
                            joueur.win(),
                            joueur.kills(),
                            joueur.deaths(),
                            joueur.assists(),
                            joueur.afk());
                })
                .toList();
        return new TeamGameDto(partie.matchId(), partie.startedAt(), partie.durationSeconds(),
                partie.queueId(), partie.queue(), partie.patch(), partie.presentPlayers(),
                partie.splitSides(), partie.win(), joueurs);
    }

    private Map<String, MemberDirectory.MemberIdentity> identites(List<TeamMember> joueurs) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        joueurs.stream()
                .map(TeamMember::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        return ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
    }

    private static MemberDirectory.MemberIdentity identite(
            Map<String, MemberDirectory.MemberIdentity> identites, TeamMember membre) {
        return membre.getUserId() == null ? null : identites.get(membre.getUserId());
    }

    private static String nomAffiche(TeamMember membre, MemberDirectory.MemberIdentity identite) {
        if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
            return identite.displayName();
        }
        return membre.riotId();
    }
}
