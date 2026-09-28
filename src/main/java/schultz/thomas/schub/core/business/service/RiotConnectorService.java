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

    Optional<HistoryWindow> historyWindow();

    // Puuid refusés par Riot (relevés avec une autre clé) depuis une date.
    List<String> stalePuuids(Instant since);

    // Rend ceux qu'on sait déjà refusés ; les autres sont vérifiés en file et apparaîtront dans stalePuuids.
    List<String> checkPuuids(java.util.Collection<String> puuids);

    HistoryWindow updateHistoryWindow(HistoryWindow window);

    List<KnownPlayer> search(String query, int limit);

    // Les joueurs dont l'historique a été relevé, en Nom#TAG : ceux qui ont une page publique.
    List<String> trackedRiotIds(int limit);

    // priorityPending : tâches demandées pour ce joueur (aperçu d'un joueur recherché) encore en file.
    record PlayerIngest(long pending, long running, Instant estimatedReadyAt, long priorityPending) {

        public PlayerIngest(long pending, long running, Instant estimatedReadyAt) {
            this(pending, running, estimatedReadyAt, 0);
        }
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

    record HistoryWindow(int maxGames, int maxAgeDays, int minGames) {
    }

    record KnownPlayer(String puuid, String gameName, String tagLine, String riotId,
                       long matchCount, List<PositionPlayed> positions, Instant lastPlayedAt,
                       Instant observedAt, String source) {
    }

    record PositionPlayed(String position, long matches) {
    }
}
