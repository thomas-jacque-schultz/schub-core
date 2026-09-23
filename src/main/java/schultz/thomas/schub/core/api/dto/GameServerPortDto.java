package schultz.thomas.schub.core.api.dto;

public record GameServerPortDto(
        String proto,
        Integer wanPort,
        Integer lanPort,
        String lanIp
) {
}
