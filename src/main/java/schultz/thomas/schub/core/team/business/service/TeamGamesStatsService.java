package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.At15Dto;
import schultz.thomas.schub.core.team.api.dto.MatchupDto;
import schultz.thomas.schub.core.team.api.dto.RankedStandingDto;
import schultz.thomas.schub.core.team.api.dto.SideRanksDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TeamGamesStatsService {

    public static final int MINIMUM_MEMBRES = 4;

    public static final int PARTIES_DEFAUT = 100;
    public static final int PARTIES_MAX = 500;
    private static final int PATCHS_RENDUS = 6;

    private static final int PARTIES_VERIFIEES = 1000;

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;

    // Silence du connecteur = RiotConnectorUnavailableException, jamais « non ».
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
        Map<String, RiotStatsGateway.Insight> insights = insightsDe(parties);
        List<TeamGameDto> rendues = parties.stream()
                .map(partie -> toGame(partie, parPuuid, identites, catalogue, insights.get(partie.matchId())).dto())
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
                bilans(decidees, TeamGamesStatsService::mode, partie -> null),
                bilans(decidees, partie -> String.valueOf(cote(partie)), partie -> null),
                bilans(decidees, RiotStatsGateway.SharedMatch::patch, partie -> null).stream()
                        .sorted(Comparator.comparing(TeamRecordDto::key, TeamGamesStatsService::parVersion).reversed())
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

    public TeamGameDetailDto detail(User actor, String teamId, String matchId) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        if (parPuuid.size() < MINIMUM_MEMBRES) {
            throw new NoSuchElementException("Cette équipe n'a pas assez de comptes Riot pour avoir des parties d'équipe");
        }
        RiotStatsGateway.SharedMatch partie = statsGateway
                .sharedMatches(List.copyOf(parPuuid.keySet()), MINIMUM_MEMBRES, null, PARTIES_VERIFIEES)
                .orElseThrow(RiotConnectorUnavailableException::new)
                .matches().stream()
                .filter(candidate -> candidate.matchId().equals(matchId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Aucune partie d'équipe « " + matchId + " »"));

        RiotStatsGateway.Insight insight = insightsDe(List.of(partie)).get(matchId);
        Partie rendue = toGame(partie, parPuuid, identites(joueurs), championGateway.catalogue(), insight);
        return new TeamGameDetailDto(team.getId(), rendue.dto(), partie.splitSides() ? List.of() : faceAFace(rendue),
                insight != null && insight.timelineAvailable(),
                insight == null ? null : insight.ranksObservedAt(),
                insight == null || partie.splitSides() ? null : EarlyGames.vue(insight.early(), cote(partie), parPuuid),
                TeamPlayerStatsService.placeDuLecteur(team, actor));
    }

    static final List<String> POSTES = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

    private static List<MatchupDto> faceAFace(Partie partie) {
        List<MatchupDto> lignes = new ArrayList<>();
        for (String poste : POSTES) {
            Optional<Joueur> allie = partie.notreCamp().stream().filter(j -> poste.equals(j.source().position())).findFirst();
            Optional<Joueur> adverse = partie.adverses().stream().filter(j -> poste.equals(j.source().position())).findFirst();
            if (allie.isEmpty() && adverse.isEmpty()) {
                continue;
            }
            lignes.add(new MatchupDto(poste,
                    allie.map(Joueur::dto).orElse(null),
                    adverse.map(Joueur::dto).orElse(null),
                    allie.isPresent() && adverse.isPresent() ? ecart(allie.get(), adverse.get()) : null));
        }
        return lignes;
    }

    static Double ecart(Joueur allie, Joueur adverse) {
        Double nous = allie.rang();
        Double eux = adverse.rang();
        return nous == null || eux == null ? null : nous - eux;
    }

    Map<String, RiotStatsGateway.Insight> insightsDe(List<RiotStatsGateway.SharedMatch> parties) {
        Map<String, RiotStatsGateway.Insight> index = new HashMap<>();
        statsGateway.insights(parties.stream().map(RiotStatsGateway.SharedMatch::matchId).toList())
                .orElseGet(List::of)
                .forEach(insight -> index.put(insight.matchId(), insight));
        return index;
    }

    record Joueur(RiotStatsGateway.SharedMatchPlayer source, RiotStatsGateway.InsightPlayer insight,
                  TeamGamePlayerDto dto) {

        Double rang() {
            return insight == null ? null : Rangs.reference(insight.solo(), insight.flex());
        }
    }

    record Partie(TeamGameDto dto, List<Joueur> membres, List<Joueur> allies, List<Joueur> adverses) {

        List<Joueur> notreCamp() {
            List<Joueur> camp = new ArrayList<>(membres);
            camp.addAll(allies);
            return camp;
        }
    }

    Partie toGame(RiotStatsGateway.SharedMatch partie,
                  Map<String, TeamMember> parPuuid,
                  Map<String, MemberDirectory.MemberIdentity> identites,
                  Optional<RiotChampionGateway.Catalogue> catalogue,
                  RiotStatsGateway.Insight insight) {
        Map<String, RiotStatsGateway.InsightPlayer> parJoueur = new HashMap<>();
        if (insight != null) {
            insight.participants().forEach(p -> parJoueur.put(p.puuid(), p));
        }
        int notreCote = cote(partie);
        Function<RiotStatsGateway.SharedMatchPlayer, Joueur> projette = joueur ->
                joueur(joueur, parPuuid, identites, catalogue, parJoueur.get(joueur.puuid()));

        List<Joueur> membres = partie.players().stream().map(projette).toList();
        List<Joueur> allies = partie.others().stream().filter(j -> j.side() == notreCote).map(projette).toList();
        List<Joueur> adverses = partie.others().stream().filter(j -> j.side() != notreCote).map(projette).toList();

        List<RiotStatsGateway.InsightPlayer> nous = new ArrayList<>();
        List<RiotStatsGateway.InsightPlayer> eux = new ArrayList<>();
        if (insight != null && insight.ranksObservedAt() != null) {
            insight.participants().forEach(p -> (p.side() == notreCote ? nous : eux).add(p));
        }

        TeamGameDto dto = new TeamGameDto(partie.matchId(), partie.startedAt(), partie.durationSeconds(),
                partie.queueId(), partie.queue(), partie.patch(), partie.presentPlayers(),
                partie.splitSides(), partie.win(),
                membres.stream().map(Joueur::dto).toList(),
                allies.stream().map(Joueur::dto).toList(),
                adverses.stream().map(Joueur::dto).toList(),
                rangs(nous), rangs(eux),
                insight == null ? null : insight.ranksObservedAt());
        return new Partie(dto, membres, allies, adverses);
    }

    private static SideRanksDto rangs(List<RiotStatsGateway.InsightPlayer> camp) {
        if (camp.isEmpty()) {
            return null;
        }
        return new SideRanksDto(
                Rangs.moyenne(camp.stream().map(RiotStatsGateway.InsightPlayer::solo).toList()),
                Rangs.moyenne(camp.stream().map(RiotStatsGateway.InsightPlayer::flex).toList()));
    }

    private static Joueur joueur(RiotStatsGateway.SharedMatchPlayer joueur,
                                 Map<String, TeamMember> parPuuid,
                                 Map<String, MemberDirectory.MemberIdentity> identites,
                                 Optional<RiotChampionGateway.Catalogue> catalogue,
                                 RiotStatsGateway.InsightPlayer insight) {
        TeamMember membre = parPuuid.get(joueur.puuid());
        RiotChampionGateway.Champion champion = catalogue
                .map(cat -> cat.parId().get(joueur.championId()))
                .orElse(null);
        return new Joueur(joueur, insight, new TeamGamePlayerDto(
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
                joueur.goldEarned(),
                joueur.damageToChampions(),
                joueur.damageTaken(),
                joueur.minionsKilled(),
                joueur.visionScore(),
                joueur.afk(),
                insight == null ? null : standing(insight.solo()),
                insight == null ? null : standing(insight.flex()),
                insight == null || insight.at15() == null ? null : at15(insight.at15())));
    }

    private static RankedStandingDto standing(RiotStatsGateway.Standing s) {
        return s == null ? null : new RankedStandingDto(s.queue(), s.riotQueueType(), s.tier(), s.division(),
                s.leaguePoints(), s.wins(), s.losses(), s.hotStreak(), s.inactive(), s.observedAt());
    }

    private static At15Dto at15(RiotStatsGateway.At15 a) {
        return new At15Dto(a.gold(), a.xp(), a.cs(), a.damageToChampions(), a.kills(), a.deaths(), a.assists());
    }

    Map<String, String> noms(List<TeamMember> joueurs) {
        Map<String, MemberDirectory.MemberIdentity> identites = identites(joueurs);
        Map<String, String> noms = new HashMap<>();
        joueurs.forEach(membre -> noms.put(membre.getMemberId(), nomAffiche(membre, identite(identites, membre))));
        return noms;
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
