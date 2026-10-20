package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;

// nextAllowedAt : null quand une mise à jour peut être demandée tout de suite.
public record StatsRefreshDto(boolean triggered, int playersQueued, @Nullable Instant nextAllowedAt) {
}
