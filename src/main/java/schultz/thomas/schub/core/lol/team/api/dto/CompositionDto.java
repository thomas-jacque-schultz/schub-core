package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record CompositionDto(
        String id,
        String teamId,
        String name,
        List<CompositionSlotDto> slots,
        List<String> bans,
        @Nullable String patch,
        @Nullable String notes,
        Instant createdAt,
        Instant updatedAt,
        boolean viewerCanEdit
) {
}
