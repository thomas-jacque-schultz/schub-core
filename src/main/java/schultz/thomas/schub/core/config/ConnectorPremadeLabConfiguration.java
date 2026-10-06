package schultz.thomas.schub.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ConnectorPremadeLabConfiguration {

    @Bean("connectorPremadeLabRestClient")
    public RestClient connectorPremadeLabRestClient(
            @Value("${connector.premadelab.base-url}") String baseUrl,
            @Value("${connector.premadelab.read-timeout:20s}") Duration readTimeout,
            @Value("${schub.internal-secret}") String internalSecret) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(readTimeout);
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }
}
