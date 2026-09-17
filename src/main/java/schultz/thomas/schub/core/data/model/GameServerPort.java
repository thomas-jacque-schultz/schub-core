package schultz.thomas.schub.core.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un port qu'un GameServer doit voir ouvert sur le routeur pendant qu'il tourne.
 * La redirection correspondante est ouverte à son allumage et refermée à son extinction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameServerPort {

    /** "tcp" ou "udp". */
    private String proto;

    /** Port ouvert côté Internet. */
    private Integer wanPort;

    /** Port visé côté LAN ; null = identique à wanPort. */
    private Integer lanPort;

    /** IP LAN visée ; null = port-forwarding.default-lan-ip. */
    private String lanIp;
}
