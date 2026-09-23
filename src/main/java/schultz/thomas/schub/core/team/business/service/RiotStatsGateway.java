package schultz.thomas.schub.core.team.business.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Optional vide = « on ne sait pas », jamais « il n'y en a pas ».
public interface RiotStatsGateway {

    enum Grouping {
        OVERALL,
        CHAMPION,
        POSITION,
        QUEUE,
        PATCH,
        MONTH,
        SIDE
    }

    enum Scope {
        ALL,
        RIFT
    }

    Optional<List<Bucket>> aggregate(List<String> puuids, Grouping groupBy, Scope scope, Instant since);

    Optional<List<References>> references(List<ReferenceRequest> joueurs);

    Optional<List<Coverage>> coverage(List<String> puuids);

    Optional<SharedMatches> sharedMatches(List<String> puuids, int minimumPlayers, Instant since,
                                          Integer limit);

    Optional<List<Standing>> rankings(String puuid);

    // Parties déjà collectées seulement : ce qui manque revient absent, jamais inventé.
    Optional<List<Insight>> insights(List<String> matchIds);

    record Insight(String matchId, boolean timelineAvailable, Instant ranksObservedAt, EarlyGame early,
                   List<InsightPlayer> participants) {
    }

    record InsightPlayer(String puuid, int side, String position, int championId, Standing solo,
                         Standing flex, At15 at15) {
    }

    record At15(int gold, int xp, int cs, int damageToChampions, int kills, int deaths, int assists) {
    }

    record EarlyGame(List<Gank> ganks, List<JunglePresence> junglers, List<Objectives> objectives) {
    }

    record Gank(int second, String lane, int attackerSide, String junglerPuuid, List<String> targetPuuids,
                String outcome, int defendersLost, int attackersLost, List<String> casualtyPuuids,
                boolean objectiveFollowUp, boolean decisive) {
    }

    record JunglePresence(String puuid, int side, int topMinutes, int midMinutes, int botMinutes) {
    }

    record Objectives(int side, int dragons, int grubs, int heralds) {
    }

    record ReferenceRequest(String puuid, String position, Instant since) {
    }

    record References(String puuid, String position, String tier, Reference league, Reference met) {
    }

    record Reference(String tier, String position, int population, int minimumGames, Map<String, Bound> bounds) {
    }

    record Bucket(
            String puuid,
            String key,
            String championName,
            long games,
            long wins,
            long kills,
            long deaths,
            long assists,
            long minionsKilled,
            long goldEarned,
            long damageToChampions,
            long damageTaken,
            long visionScore,
            long teamKills,
            long teamDeaths,
            long afkGames,
            long secondsPlayed,
            Instant firstPlayedAt,
            Instant lastPlayedAt
    ) {
    }

    record Coverage(
            String puuid,
            boolean tracked,
            long knownMatches,
            long analysedMatches,
            Instant firstPlayedAt,
            Instant lastPlayedAt,
            Instant lastSyncAt
    ) {
    }

    record SharedMatches(int minimumPlayers, long totalMatches, boolean truncated,
                         List<SharedMatch> matches) {
    }

    record SharedMatch(
            String matchId,
            Instant startedAt,
            long durationSeconds,
            int queueId,
            String queue,
            String patch,
            int presentPlayers,
            boolean splitSides,
            Boolean win,
            List<SharedMatchPlayer> players,
            List<SharedMatchPlayer> others
    ) {
    }

    record SharedMatchPlayer(
            String puuid,
            int championId,
            String championName,
            String position,
            boolean win,
            int side,
            int kills,
            int deaths,
            int assists,
            int minionsKilled,
            int goldEarned,
            int damageToChampions,
            int damageTaken,
            int visionScore,
            boolean afk
    ) {
    }

    record Bound(double low, double high) {
    }

    record Standing(String queue, String riotQueueType, String tier, String division, int leaguePoints,
                    int wins, int losses, boolean hotStreak, boolean inactive, Instant observedAt) {
    }
}
