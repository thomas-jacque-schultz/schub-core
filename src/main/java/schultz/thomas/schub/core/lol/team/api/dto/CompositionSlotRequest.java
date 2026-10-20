package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.data.model.GameRole;

import jakarta.annotation.Nullable;
import java.util.List;

public record CompositionSlotRequest(GameRole role, String championId, @Nullable String memberId,
                                     List<String> alternatives) {
}
