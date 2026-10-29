package schultz.thomas.schub.core.lol.team.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MacroPlanDto(String id, String teamId, String name, List<Map<String, Object>> elements,
                           Instant createdAt, Instant updatedAt) {
}
