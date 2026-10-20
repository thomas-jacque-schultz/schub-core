package schultz.thomas.schub.core.api.dto;

import java.util.List;

// unknown : les Riot ID saisis que le connecteur ne connaît pas.
public record SeoCreatorsDto(List<String> creators, List<String> unknown) {
}
