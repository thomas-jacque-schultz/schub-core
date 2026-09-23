package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.team.api.dto.At15Dto;
import schultz.thomas.schub.core.team.api.dto.GamePlayerMetricsDto;
import schultz.thomas.schub.core.team.api.dto.MatchupDto;
import schultz.thomas.schub.core.team.api.dto.RankedStandingDto;
import schultz.thomas.schub.core.team.api.dto.SideRanksDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamePlayerDto;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

// Une partie vue depuis « nos » joueurs — les membres d'une équipe, ou soi seul : leur camp, les adversaires.
@Service
@RequiredArgsConstructor
public class GameViews {

    static final List<String> POSTES = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;

    List<TeamGameDto> parties(List<RiotStatsGateway.SharedMatch> parties, Map<String, TeamMember> parPuuid,
                              Map<String, String> noms) {
        Optional<RiotChampionGateway.Catalogue> catalogue = championGateway.catalogue();
        Map<String, RiotStatsGateway.Insight> insights = insights(parties);
        return parties.stream()
                .map(partie -> partie(partie, parPuuid, noms, catalogue, insights.get(partie.matchId())).dto())
                .toList();
    }

    TeamGameDetailDto detail(String teamId, RiotStatsGateway.SharedMatch partie, Map<String, TeamMember> parPuuid,
                             Map<String, String> noms, String viewerMemberId, Integer days) {
        RiotStatsGateway.Insight insight = insights(List.of(partie)).get(partie.matchId());
        Partie rendue = partie(partie, parPuuid, noms, championGateway.catalogue(), insight);
        return new TeamGameDetailDto(teamId, rendue.dto(), partie.splitSides() ? List.of() : faceAFace(rendue),
                insight != null && insight.timelineAvailable(),
                insight == null ? null : insight.ranksObservedAt(),
                insight == null || partie.splitSides() ? null
                        : EarlyGames.vue(insight.early(), cote(partie), parPuuid),
                viewerMemberId,
                days,
                indicateurs(partie, PlayerStatsService.depuis(days)));
    }

    Map<String, RiotStatsGateway.Insight> insights(List<RiotStatsGateway.SharedMatch> parties) {
        Map<String, RiotStatsGateway.Insight> index = new HashMap<>();
        statsGateway.insights(parties.stream().map(RiotStatsGateway.SharedMatch::matchId).toList())
                .orElseGet(List::of)
                .forEach(insight -> index.put(insight.matchId(), insight));
        return index;
    }

    static int cote(RiotStatsGateway.SharedMatch partie) {
        return partie.players().isEmpty() ? 0 : partie.players().getFirst().side();
    }

    // La partie elle-même, puis la moyenne du joueur au même poste sur la période : une valeur n'a de sens qu'à son poste.
    private List<GamePlayerMetricsDto> indicateurs(RiotStatsGateway.SharedMatch partie, Instant since) {
        List<RiotStatsGateway.PlayerMetrics> joueurs = statsGateway.matchMetrics(partie.matchId()).orElseGet(List::of);
        if (joueurs.isEmpty()) {
            return List.of();
        }
        Map<String, Map<String, RiotStatsGateway.Bucket>> moyennes = new HashMap<>();
        statsGateway.aggregate(joueurs.stream().map(RiotStatsGateway.PlayerMetrics::puuid).toList(),
                        RiotStatsGateway.Grouping.POSITION, RiotStatsGateway.Scope.RIFT, since)
                .orElseGet(List::of)
                .forEach(bucket -> moyennes.computeIfAbsent(bucket.puuid(), puuid -> new HashMap<>())
                        .put(bucket.key(), bucket));
        return joueurs.stream()
                .map(joueur -> {
                    RiotStatsGateway.Bucket moyenne = moyennes.getOrDefault(joueur.puuid(), Map.of())
                            .get(joueur.position());
                    return new GamePlayerMetricsDto(joueur.side(), joueur.championId(), joueur.position(),
                            joueur.tier(), joueur.tierEstimated(), joueur.values(),
                            moyenne == null ? null : StatLines.of(moyenne, null, null, null));
                })
                .toList();
    }

    private static List<MatchupDto> faceAFace(Partie partie) {
        List<MatchupDto> lignes = new ArrayList<>();
        for (String poste : POSTES) {
            Optional<Joueur> allie = partie.notreCamp().stream()
                    .filter(j -> poste.equals(j.source().position())).findFirst();
            Optional<Joueur> adverse = partie.adverses().stream()
                    .filter(j -> poste.equals(j.source().position())).findFirst();
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

    Partie partie(RiotStatsGateway.SharedMatch partie, Map<String, TeamMember> parPuuid, Map<String, String> noms,
                  Optional<RiotChampionGateway.Catalogue> catalogue, RiotStatsGateway.Insight insight) {
        Map<String, RiotStatsGateway.InsightPlayer> parJoueur = new HashMap<>();
        if (insight != null) {
            insight.participants().forEach(p -> parJoueur.put(p.puuid(), p));
        }
        int notreCote = cote(partie);
        Function<RiotStatsGateway.SharedMatchPlayer, Joueur> projette = joueur ->
                joueur(joueur, parPuuid, noms, catalogue, parJoueur.get(joueur.puuid()));

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

    private static Joueur joueur(RiotStatsGateway.SharedMatchPlayer joueur, Map<String, TeamMember> parPuuid,
                                 Map<String, String> noms, Optional<RiotChampionGateway.Catalogue> catalogue,
                                 RiotStatsGateway.InsightPlayer insight) {
        TeamMember membre = parPuuid.get(joueur.puuid());
        RiotChampionGateway.Champion champion = catalogue
                .map(cat -> cat.parId().get(joueur.championId()))
                .orElse(null);
        return new Joueur(joueur, insight, new TeamGamePlayerDto(
                membre == null ? null : membre.getMemberId(),
                membre == null ? null : noms.getOrDefault(membre.getMemberId(), membre.riotId()),
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
}
