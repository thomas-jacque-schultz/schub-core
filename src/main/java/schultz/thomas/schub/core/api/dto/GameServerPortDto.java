package schultz.thomas.schub.core.api.dto;

import jakarta.annotation.Nullable;

public record GameServerPortDto(
        String proto,
        Integer wanPort,
        @Nullable Integer lanPort,
        @Nullable String lanIp
) {
}
