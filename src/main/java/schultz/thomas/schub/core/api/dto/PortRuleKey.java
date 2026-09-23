package schultz.thomas.schub.core.api.dto;

public record PortRuleKey(Protocol protocol, int wanPortStart, int wanPortEnd) {
}
