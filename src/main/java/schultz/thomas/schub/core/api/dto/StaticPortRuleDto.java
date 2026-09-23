package schultz.thomas.schub.core.api.dto;

public record StaticPortRuleDto(
        String id,
        String name,
        String proto,
        Integer wanPortStart,
        Integer wanPortEnd,
        Integer lanPort,
        String lanIp,
        boolean enabled
) {
}
