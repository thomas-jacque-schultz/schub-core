package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record CompositionDto(
        String id,
        String teamId,
        String name,
        List<CompositionSlotDto> slots,
        String patch,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        boolean viewerCanEdit
) {
}
