package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "connector.riot")
public class ConnectorRiotProperties {

    private String baseUrl = "http://connector-riot:8080";

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(5);

    // Sous les 60 s de lecture du BFF (Feign) : l'effacement des données Riot supprime des collections entières.
    private Duration purgeTimeout = Duration.ofSeconds(50);
}
