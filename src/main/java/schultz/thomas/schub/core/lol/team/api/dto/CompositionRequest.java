package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;
import java.util.List;

public record CompositionRequest(String name, List<CompositionSlotRequest> slots, List<String> bans,
                                 @Nullable String patch, @Nullable String notes) {
}
