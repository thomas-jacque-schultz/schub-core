package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Protocol;

public record PortRuleDto(
        String providerId,
        String owner,
        Protocol protocol,
        int wanPortStart,
        int wanPortEnd,
        String lanIp,
        int lanPort,
        boolean open
) {
}
