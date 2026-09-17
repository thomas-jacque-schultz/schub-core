package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une redirection permanente, voulue indépendamment de tout serveur de jeu.
 *
 * <p>Ces règles vivaient dans {@code /etc/schub/port-forwarding.yml}, lu au démarrage. Elles
 * sont passées en base pour être modifiables depuis l'interface : un fichier relu au boot ne
 * permet ni d'ajouter ni de supprimer sans redémarrer.</p>
 *
 * <p>L'index unique reproduit la contrainte du routeur lui-même, qui ne peut pas porter deux
 * redirections sur le même protocole et le même port WAN. Mieux vaut le refuser à l'écriture
 * que de laisser le réconciliateur découvrir le conflit devant la box.</p>
 */
@Data
@Document(collection = "static_port_rules")
@CompoundIndex(name = "proto_wan_unique", def = "{'proto': 1, 'wanPortStart': 1}", unique = true)
public class StaticPortRuleEntity {

    @Id
    private String id;

    /** Libellé humain ; devient {@code static/<name>} comme propriétaire de la règle. */
    private String name;

    /** "tcp" ou "udp". */
    private String proto;

    private Integer wanPortStart;

    /** Absent = redirection d'un port unique. */
    private Integer wanPortEnd;

    /** Absent = identique au port WAN. */
    private Integer lanPort;

    /** Absent = port-forwarding.default-lan-ip. */
    private String lanIp;

    /** Permet de fermer une règle sans la supprimer. */
    private boolean enabled = true;
}
