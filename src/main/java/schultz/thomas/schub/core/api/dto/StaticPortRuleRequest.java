package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;

public record StaticPortRuleRequest(
        String name,
        String proto,
        Integer wanPortStart,
        @Nullable Integer wanPortEnd,
        @Nullable Integer lanPort,
        @Nullable String lanIp,
        boolean enabled
) {
}
