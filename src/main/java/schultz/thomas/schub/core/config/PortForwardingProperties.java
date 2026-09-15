package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Politique d'ouverture des ports : quoi ouvrir, vers où, et ce qui reste interdit.
 *
 * <p>Rien ici ne dépend de la marque du routeur : depuis la phase 1, celle-ci est entièrement
 * confinée dans {@code schub-connector-freebox}.</p>
 */
@Data
@ConfigurationProperties(prefix = "port-forwarding")
public class PortForwardingProperties {

    /** Coupe-circuit global : à false, le routeur n'est jamais interrogé. */
    private boolean enabled = false;

    /** Journalise les changements sans jamais les écrire. Utile pour un premier run. */
    private boolean dryRun = false;

    /** IP LAN visée par défaut quand une règle n'en précise pas. */
    private String defaultLanIp = "";

    /**
     * Supprime les règles marquées qui ne correspondent plus à aucune règle voulue.
     * À false (défaut) elles sont seulement fermées, ce qui en laisse une trace lisible.
     */
    private boolean pruneOrphans = false;

    /** Ports que le réconciliateur refusera toujours d'ouvrir, quelle que soit la configuration. */
    private List<Integer> forbiddenWanPorts = new ArrayList<>(List.of(22, 23, 139, 445, 3389, 5432, 27017, 9443));

    /** Règles permanentes, éditées à la main. Toujours ouvertes tant qu'elles sont listées ici. */
    private List<StaticRule> staticRules = new ArrayList<>();

    /** Une règle permanente déclarée en configuration. */
    @Data
    public static class StaticRule {
        /** Libellé repris comme propriétaire de la règle. */
        private String name;
        /** "tcp" ou "udp". */
        private String proto = "tcp";
        private Integer wanPortStart;
        /** Absent = redirection d'un port unique. */
        private Integer wanPortEnd;
        private Integer lanPort;
        /** Absent = defaultLanIp. */
        private String lanIp;
        /** Permet de fermer une règle sans la retirer du fichier. */
        private boolean enabled = true;
    }
}
