package schultz.thomas.schub.core.business.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RiotConnectorService {

    Optional<PlayerIngest> ingestOf(String puuid);

    boolean requestIngest(String puuid);

    // Joueur recherché : ses dernières parties d'abord ; slow, au compte-gouttes.
    boolean requestPreview(String puuid, boolean slow);

    Optional<IngestLoad> load();

    Optional<IngestSummary> summary();

    Optional<Crawler> crawler();

    Crawler toggleCrawler(boolean enabled);

    List<KnownPlayer> search(String query, int limit);

    record PlayerIngest(long pending, long running, Instant estimatedReadyAt) {
    }

    record IngestSummary(Counts matches, Counts profiles) {

        public record Counts(long retrieved, long analysed, long pending) {
        }
    }

    record IngestLoad(long pending, long running, long failed, double callsPerMinute,
                      Duration estimatedDrain, Instant estimatedReadyAt, Duration throttledFor) {
    }

    record Crawler(boolean switchable, boolean enabled, boolean running, long knownAccounts,
                   long trackedAccounts, long backgroundPending, long databaseBytes, long storageAlertBytes,
                   boolean storageAlert, Instant lastRoundAt, int lastRoundAccounts) {
    }

    record KnownPlayer(String puuid, String gameName, String tagLine, String riotId,
                       long matchCount, List<PositionPlayed> positions, Instant lastPlayedAt,
                       Instant observedAt, String source) {
    }

    record PositionPlayed(String position, long matches) {
    }
}
