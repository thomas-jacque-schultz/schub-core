package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Protocol;

import jakarta.annotation.Nullable;

public record PortRuleDto(
        @Nullable String providerId,
        @Nullable String owner,
        Protocol protocol,
        int wanPortStart,
        int wanPortEnd,
        String lanIp,
        int lanPort,
        boolean open
) {
}
