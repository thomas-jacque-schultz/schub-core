package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "connector.freebox")
public class ConnectorFreeboxProperties {

    private String baseUrl = "http://connector-freebox:8080";

    // Un peu plus larges que ceux du connecteur vers la box, qu'ils englobent.
    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(8);
}
