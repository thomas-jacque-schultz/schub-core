package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

public record CompositionSlotRequest(GameRole role, String championId, String memberId) {
}
