package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Comment joindre le connecteur Portainer.
 *
 * <p>Remplace l'ancien bloc {@code portainer.*} : depuis la phase 2, ce service ne parle plus à
 * Portainer mais à un connecteur qui, lui, le sonde une fois par minute pour tout le monde.</p>
 */
@Data
@ConfigurationProperties(prefix = "connector.portainer")
public class ConnectorPortainerProperties {

    private String baseUrl = "http://connector-portainer:8080";

    /**
     * Âge au-delà duquel une lecture du connecteur n'est plus considérée comme un état.
     *
     * <p>Le connecteur sert un cache : une valeur trop ancienne doit être traitée comme une
     * absence de réponse, pas comme « le serveur est éteint » (plan §6). Doit rester supérieur
     * à la période de sonde du connecteur, sinon toute lecture serait déclarée périmée.</p>
     */
    private Duration staleAfter = Duration.ofMinutes(5);

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);
}
