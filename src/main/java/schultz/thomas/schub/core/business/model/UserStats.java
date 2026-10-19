package schultz.thomas.schub.core.business.model;

import java.util.List;
import java.util.Map;

// searchers : personnes distinctes qui ont utilisé la recherche ce jour-là, sans compte ni cookie.
public record UserStats(long total, long riotLinked, Active active, Map<String, Active> activeByApp,
                           List<Day> daily) {

    public record Active(long last24h, long last7d, long last30d) {
    }

    public record Day(String day, long activeUsers, long searchers) {
    }
}
