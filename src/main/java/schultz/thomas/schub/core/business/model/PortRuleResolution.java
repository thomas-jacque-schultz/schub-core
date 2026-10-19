package schultz.thomas.schub.core.business.model;

import java.util.List;
import java.util.Map;

public record PortRuleResolution(Map<PortRuleKey, PortRule> rules, List<String> rejected) {
}
