package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "connector.portainer")
public class ConnectorPortainerProperties {

    private String baseUrl = "http://connector-portainer:8080";

    private Duration staleAfter = Duration.ofMinutes(5);

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);
}
