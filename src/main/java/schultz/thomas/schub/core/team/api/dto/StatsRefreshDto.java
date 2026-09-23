package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

// nextAllowedAt : null quand une mise à jour peut être demandée tout de suite.
public record StatsRefreshDto(boolean triggered, int playersQueued, Instant nextAllowedAt) {
}
