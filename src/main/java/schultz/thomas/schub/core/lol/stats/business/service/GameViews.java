package schultz.thomas.schub.core.lol.stats.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.lol.riot.business.service.RiotChampionGateway;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.riot.business.service.VueDePartie;
import schultz.thomas.schub.core.lol.stats.business.model.GamePlayerMetrics;
import schultz.thomas.schub.core.lol.stats.business.model.Matchup;
import schultz.thomas.schub.core.lol.stats.business.model.RankedStanding;
import schultz.thomas.schub.core.lol.stats.business.model.SideRanks;
import schultz.thomas.schub.core.lol.stats.business.model.TeamGame;
import schultz.thomas.schub.core.lol.stats.business.model.TeamGameDetail;
import schultz.thomas.schub.core.lol.stats.business.model.TeamGamePlayer;
import schultz.thomas.schub.core.lol.team.data.model.GameRole;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

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

    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;

    List<TeamGame> parties(List<RiotStatsGateway.SharedMatch> parties, Map<String, TeamMember> parPuuid,
                              Map<String, String> noms) {
        Optional<RiotChampionGateway.Catalogue> catalogue = championGateway.catalogue();
        Map<String, RiotStatsGateway.Insight> insights = insights(parties);
        return parties.stream()
                .map(partie -> partie(partie, parPuuid, noms, catalogue, insights.get(partie.matchId())).dto())
                .toList();
    }

    TeamGameDetail detail(String teamId, RiotStatsGateway.SharedMatch partie, Map<String, TeamMember> parPuuid,
                             Map<String, String> noms, String viewerMemberId, Integer days) {
        RiotStatsGateway.Insight insight = insights(List.of(partie)).get(partie.matchId());
        Partie rendue = partie(partie, parPuuid, noms, championGateway.catalogue(), insight);
        return new TeamGameDetail(teamId, rendue.dto(), partie.splitSides() ? List.of() : faceAFace(rendue),
                insight != null && insight.timelineAvailable(),
                insight == null ? null : insight.ranksObservedAt(),
                insight == null || partie.splitSides() ? null
                        : EarlyGames.vue(insight.early(), VueDePartie.de(partie).cote(), parPuuid,
                        partie.players()),
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

    // La partie elle-même, puis la moyenne du joueur au même poste sur la période : une valeur n'a de sens qu'à son poste.
    private List<GamePlayerMetrics> indicateurs(RiotStatsGateway.SharedMatch partie, Instant since) {
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
                    return new GamePlayerMetrics(joueur.side(), joueur.championId(), joueur.position(),
                            joueur.tier(), joueur.tierEstimated(), joueur.values(),
                            moyenne == null ? null : StatLines.of(moyenne, null, null, null));
                })
                .toList();
    }

    private static List<Matchup> faceAFace(Partie partie) {
        List<Matchup> lignes = new ArrayList<>();
        for (String poste : GameRole.POSITIONS) {
            Optional<Joueur> allie = VueDePartie.auPoste(partie.notreCamp(), poste);
            Optional<Joueur> adverse = VueDePartie.auPoste(partie.adverses(), poste);
            if (allie.isEmpty() && adverse.isEmpty()) {
                continue;
            }
            lignes.add(new Matchup(poste,
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
                  TeamGamePlayer dto) implements RiotStatsGateway.Participant {

        @Override
        public String puuid() {
            return source.puuid();
        }

        @Override
        public int side() {
            return source.side();
        }

        @Override
        public String position() {
            return source.position();
        }

        Double rang() {
            return insight == null ? null : Rangs.reference(insight.solo(), insight.flex());
        }
    }

    record Partie(TeamGame dto, List<Joueur> membres, List<Joueur> allies, List<Joueur> adverses) {

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
        VueDePartie vue = VueDePartie.de(partie);
        Function<RiotStatsGateway.SharedMatchPlayer, Joueur> projette = joueur ->
                joueur(joueur, parPuuid, noms, catalogue, parJoueur.get(joueur.puuid()));

        List<Joueur> membres = partie.players().stream().map(projette).toList();
        List<Joueur> allies = vue.allies().stream().map(projette).toList();
        List<Joueur> adverses = vue.adverses().stream().map(projette).toList();

        boolean rangsReleves = insight != null && insight.ranksObservedAt() != null;
        List<RiotStatsGateway.InsightPlayer> nous = rangsReleves
                ? VueDePartie.camp(insight.participants(), vue.cote(), true) : List.of();
        List<RiotStatsGateway.InsightPlayer> eux = rangsReleves
                ? VueDePartie.camp(insight.participants(), vue.cote(), false) : List.of();

        TeamGame dto = new TeamGame(partie.matchId(), partie.startedAt(), partie.durationSeconds(),
                partie.queueId(), partie.queue(), partie.patch(), partie.presentPlayers(),
                partie.splitSides(), partie.win(),
                membres.stream().map(Joueur::dto).toList(),
                allies.stream().map(Joueur::dto).toList(),
                adverses.stream().map(Joueur::dto).toList(),
                rangs(nous), rangs(eux),
                insight == null ? null : insight.ranksObservedAt());
        return new Partie(dto, membres, allies, adverses);
    }

    private static SideRanks rangs(List<RiotStatsGateway.InsightPlayer> camp) {
        if (camp.isEmpty()) {
            return null;
        }
        return new SideRanks(
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
        return new Joueur(joueur, insight, new TeamGamePlayer(
                membre == null ? null : membre.getMemberId(),
                membre == null ? null : noms.getOrDefault(membre.getMemberId(), membre.riotId()),
                membre != null && membre.riotId() != null ? membre.riotId() : joueur.riotId(),
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
                insight == null ? null : insight.at15()));
    }

    private static RankedStanding standing(RiotStatsGateway.Standing s) {
        return s == null ? null : new RankedStanding(s.queue(), s.riotQueueType(), s.tier(), s.division(),
                s.leaguePoints(), s.wins(), s.losses(), s.hotStreak(), s.inactive(), s.observedAt());
    }
}
