package schultz.thomas.schub.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PortForwardingPropertiesBindingTest {

    @Configuration
    @EnableConfigurationProperties(PortForwardingProperties.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("les règles permanentes du fichier importé sont bien chargées")
    void loadsStaticRulesFromImportedFile(@TempDir Path tempDir) throws Exception {
        Path rulesFile = tempDir.resolve("port-forwarding.yml");
        Files.writeString(rulesFile, """
                port-forwarding:
                  static-rules:
                    - name: wireguard
                      proto: udp
                      wan-port-start: 51820
                      lan-port: 51820
                      lan-ip: 192.168.1.200
                    - name: teamspeak
                      proto: udp
                      wan-port-start: 9987
                      wan-port-end: 9989
                      enabled: false
                """);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfig.class)
                .web(WebApplicationType.NONE)
                .properties("spring.config.import=file:" + rulesFile)
                .run()) {

            PortForwardingProperties properties = context.getBean(PortForwardingProperties.class);

            assertThat(properties.getStaticRules()).hasSize(2);

            PortForwardingProperties.StaticRule wireguard = properties.getStaticRules().get(0);
            assertThat(wireguard.getName()).isEqualTo("wireguard");
            assertThat(wireguard.getProto()).isEqualTo("udp");
            assertThat(wireguard.getWanPortStart()).isEqualTo(51820);
            assertThat(wireguard.getLanIp()).isEqualTo("192.168.1.200");
            assertThat(wireguard.isEnabled()).isTrue();

            PortForwardingProperties.StaticRule teamspeak = properties.getStaticRules().get(1);
            assertThat(teamspeak.getWanPortEnd()).isEqualTo(9989);
            assertThat(teamspeak.isEnabled()).isFalse();
        }
    }

    @Test
    @DisplayName("l'absence du fichier monté ne bloque pas le démarrage")
    void startsWithoutRulesFile(@TempDir Path tempDir) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfig.class)
                .web(WebApplicationType.NONE)
                .properties("spring.config.import=optional:file:" + tempDir.resolve("absent.yml"))
                .run()) {

            assertThat(context.getBean(PortForwardingProperties.class).getStaticRules()).isEmpty();
        }
    }
}
