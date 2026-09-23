package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.service.RiotConnectorService.Crawler;

import java.time.Instant;

public record CrawlerDto(
        boolean available,
        boolean switchable,
        boolean enabled,
        boolean running,
        long knownAccounts,
        long trackedAccounts,
        long backgroundPending,
        long databaseBytes,
        long storageAlertBytes,
        boolean storageAlert,
        Instant lastRoundAt,
        int lastRoundAccounts) {

    public static CrawlerDto from(Crawler c) {
        return new CrawlerDto(true, c.switchable(), c.enabled(), c.running(), c.knownAccounts(),
                c.trackedAccounts(), c.backgroundPending(), c.databaseBytes(), c.storageAlertBytes(),
                c.storageAlert(), c.lastRoundAt(), c.lastRoundAccounts());
    }

    public static CrawlerDto unavailable() {
        return new CrawlerDto(false, false, false, false, 0, 0, 0, 0, 0, false, null, 0);
    }
}
