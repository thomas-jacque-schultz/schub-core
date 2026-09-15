package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Comment joindre le connecteur Freebox.
 *
 * <p>Remplace l'ancien bloc {@code freebox.*} : depuis la phase 1, ce service ne parle plus
 * à la box mais à un connecteur qui, lui, sait le faire. Rien ici ne mentionne la marque du
 * routeur — c'est précisément le but de la découpe.</p>
 */
@Data
@ConfigurationProperties(prefix = "connector.freebox")
public class ConnectorFreeboxProperties {

    /** Nom court du service sur l'overlay, ou URL complète en développement. */
    private String baseUrl = "http://connector-freebox:8080";

    /**
     * Bornes sur les échanges. Un connecteur muet ne doit pas bloquer le démarrage d'un
     * serveur de jeu : l'appel échoue vite et la réconciliation périodique repassera.
     * Un peu plus large que les timeouts du connecteur vers la box, qu'il englobe.
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(8);
}
