package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

public record TeamRecordDto(
        String key,
        @Nullable String label,
        long games,
        long wins,
        long losses,
        @Nullable Double winRate,
        @Nullable Double averageDurationSeconds
) {
}
