package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

public record CompositionRequest(String name, List<CompositionSlotRequest> slots, String patch, String notes) {
}
