package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

public record MacroPlanRequest(@Nullable String name, @Nullable List<Map<String, Object>> elements) {
}
