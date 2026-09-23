package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

import java.util.List;

public record CompositionSlotDto(
        GameRole role,
        String championId,
        String memberId,
        String playerDisplayName,
        List<String> alternatives
) {
}
