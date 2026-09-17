package schultz.thomas.schub.core.api.dto;

/** Port à ouvrir sur le routeur pendant que le serveur tourne. */
public record GameServerPortDto(
        /** "tcp" ou "udp" */
        String proto,
        /** port ouvert côté Internet */
        Integer wanPort,
        /** port visé côté LAN ; null = identique à wanPort */
        Integer lanPort,
        /** IP LAN visée ; null = valeur par défaut de la configuration */
        String lanIp
) {
}
