package schultz.thomas.schub.core.api.dto;

public record StaticPortRuleRequest(
        String name,
        String proto,
        Integer wanPortStart,
        Integer wanPortEnd,
        Integer lanPort,
        String lanIp,
        boolean enabled
) {
}
