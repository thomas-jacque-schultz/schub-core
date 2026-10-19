package schultz.thomas.schub.core.team.business.model;

import java.time.Instant;

// nextAllowedAt : null quand une mise à jour peut être demandée tout de suite.
public record StatsRefresh(boolean triggered, int playersQueued, Instant nextAllowedAt) {
}
