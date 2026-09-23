package schultz.thomas.schub.core.business.model;

import schultz.thomas.schub.core.api.dto.PortRule;
import schultz.thomas.schub.core.api.dto.PortRuleKey;

import java.util.List;
import java.util.Map;

public record PortRuleResolution(Map<PortRuleKey, PortRule> rules, List<String> rejected) {
}
