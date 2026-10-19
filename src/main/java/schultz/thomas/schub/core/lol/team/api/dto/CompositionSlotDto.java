package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.data.model.GameRole;

import java.util.List;

public record CompositionSlotDto(
        GameRole role,
        String championId,
        String memberId,
        String playerDisplayName,
        List<String> alternatives
) {
}
