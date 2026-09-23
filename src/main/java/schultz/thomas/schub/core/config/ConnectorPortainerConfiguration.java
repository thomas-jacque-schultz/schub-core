package schultz.thomas.schub.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(ConnectorPortainerProperties.class)
public class ConnectorPortainerConfiguration {

    private final ConnectorPortainerProperties properties;

    @Value("${schub.internal-secret}")
    private String internalSecret;

    @Bean("connectorPortainerRestClient")
    public RestClient connectorPortainerRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }
}
