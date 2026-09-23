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

    Optional<Scale> scale();

    Optional<List<Coverage>> coverage(List<String> puuids);

    Optional<SharedMatches> sharedMatches(List<String> puuids, int minimumPlayers, Instant since,
                                          Integer limit);

    Optional<List<Standing>> rankings(String puuid);

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

    record Scale(Instant computedAt, int population, int minimumGames, List<String> recentPatches,
                 Map<String, Bound> bounds) {
    }

    record Bound(double low, double high) {
    }

    record Standing(String queue, String riotQueueType, String tier, String division, int leaguePoints,
                    int wins, int losses, boolean hotStreak, boolean inactive, Instant observedAt) {
    }
}
