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
@EnableConfigurationProperties({ConnectorFreeboxProperties.class, PortForwardingProperties.class})
public class ConnectorFreeboxConfiguration {

    private final ConnectorFreeboxProperties properties;

    @Value("${schub.internal-secret}")
    private String internalSecret;

    @Bean("connectorFreeboxRestClient")
    public RestClient connectorFreeboxRestClient() {
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
