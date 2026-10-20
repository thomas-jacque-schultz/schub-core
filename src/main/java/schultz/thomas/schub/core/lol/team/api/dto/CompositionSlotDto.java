package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.data.model.GameRole;

import jakarta.annotation.Nullable;
import java.util.List;

public record CompositionSlotDto(
        GameRole role,
        @Nullable String championId,
        @Nullable String memberId,
        @Nullable String playerDisplayName,
        List<String> alternatives
) {
}
