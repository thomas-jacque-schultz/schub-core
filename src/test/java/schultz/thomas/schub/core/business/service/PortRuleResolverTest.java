package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.api.dto.PortRule;
import schultz.thomas.schub.core.api.dto.Protocol;
import schultz.thomas.schub.core.business.model.PortRuleResolution;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.config.PortForwardingProperties;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.GameServerPort;
import schultz.thomas.schub.core.data.model.GameServerStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Le calcul de l'état voulu ne touche à rien : ces tests valident la politique
 * (ports interdits, doublons, valeurs par défaut) sans routeur ni HTTP.
 */
@ExtendWith(MockitoExtension.class)
class PortRuleResolverTest {

    @Mock
    private GameServerService gameServerService;

    private PortForwardingProperties properties;

    private PortRuleResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new PortForwardingProperties();
        properties.setDefaultLanIp("192.168.1.202");
        // Le fournisseur pointe sur les propriétés : chaque test continue de déclarer ses
        // règles permanentes via properties.setStaticRules(...), comme avant leur passage
        // en base. Le resolver ne sait pas d'où vient la liste, c'est tout l'intérêt.
        resolver = new PortRuleResolver(properties, gameServerService, properties::getStaticRules);
    }

    @Test
    @DisplayName("un serveur ONLINE veut ses ports ouverts, avec les valeurs par défaut")
    void openPortsForOnlineServer() {
        givenServers(server("minecraft-ftb", GameServerStatus.ONLINE, port("tcp", 15007, null, null)));

        PortRule rule = onlyRule(resolver.resolve(null, false));

        assertThat(rule.owner()).isEqualTo("minecraft-ftb");
        assertThat(rule.protocol()).isEqualTo(Protocol.TCP);
        assertThat(rule.wanPortStart()).isEqualTo(15007);
        assertThat(rule.wanPortEnd()).isEqualTo(15007);
        assertThat(rule.lanPort()).isEqualTo(15007);
        assertThat(rule.lanIp()).isEqualTo("192.168.1.202");
        assertThat(rule.open()).isTrue();
        assertThat(rule.providerId()).isNull();
    }

    @Test
    @DisplayName("un serveur éteint veut ses ports fermés, pas absents")
    void closedPortsForOfflineServer() {
        givenServers(server("palworld-miam", GameServerStatus.OFFLINE, port("udp", 8211, null, null)));

        assertThat(onlyRule(resolver.resolve(null, false)).open()).isFalse();
    }

    @Test
    @DisplayName("l'override l'emporte sur le statut observé, et ne vaut que pour ce serveur")
    void overrideWinsForTargetedServerOnly() {
        givenServers(
                server("minecraft-ftb", GameServerStatus.OFFLINE, port("tcp", 15007, null, null)),
                server("palworld-miam", GameServerStatus.OFFLINE, port("udp", 8211, null, null))
        );

        PortRuleResolution resolution = resolver.resolve("minecraft-ftb", true);

        assertThat(byOwner(resolution, "minecraft-ftb").open()).isTrue();
        assertThat(byOwner(resolution, "palworld-miam").open()).isFalse();
    }

    @Test
    @DisplayName("un port de la liste interdite est écarté avec son motif")
    void rejectsForbiddenPort() {
        givenServers(server("mauvaise-idee", GameServerStatus.ONLINE, port("tcp", 22, null, null)));

        PortRuleResolution resolution = resolver.resolve(null, false);

        assertThat(resolution.rules()).isEmpty();
        assertThat(resolution.rejected()).singleElement().asString()
                .contains("mauvaise-idee").contains("22").contains("interdit");
    }

    @Test
    @DisplayName("une plage qui recouvre un port interdit est écartée aussi")
    void rejectsRangeCoveringForbiddenPort() {
        properties.setStaticRules(List.of(staticRule("large", "tcp", 20, 30, null, null)));
        givenNoServers();

        assertThat(resolver.resolve(null, false).rules()).isEmpty();
    }

    @Test
    @DisplayName("un protocole inconnu est écarté")
    void rejectsUnknownProtocol() {
        givenServers(server("bizarre", GameServerStatus.ONLINE, port("sctp", 1234, null, null)));

        assertThat(resolver.resolve(null, false).rejected()).singleElement().asString().contains("sctp");
    }

    @Test
    @DisplayName("deux serveurs sur le même port WAN : le premier garde le port, le second est écarté")
    void rejectsDuplicateWanPort() {
        givenServers(
                server("jeu-a", GameServerStatus.ONLINE, port("tcp", 25565, null, null)),
                server("jeu-b", GameServerStatus.ONLINE, port("tcp", 25565, null, null))
        );

        PortRuleResolution resolution = resolver.resolve(null, false);

        assertThat(resolution.rules()).hasSize(1);
        assertThat(onlyRule(resolution).owner()).isEqualTo("jeu-a");
        assertThat(resolution.rejected()).singleElement().asString().startsWith("jeu-b");
    }

    @Test
    @DisplayName("sans IP LAN ni valeur par défaut, la règle est écartée plutôt que devinée")
    void rejectsWhenNoLanIpAvailable() {
        properties.setDefaultLanIp("");
        givenServers(server("minecraft-ftb", GameServerStatus.ONLINE, port("tcp", 15007, null, null)));

        assertThat(resolver.resolve(null, false).rejected()).singleElement().asString().contains("IP LAN");
    }

    @Test
    @DisplayName("l'IP et le port LAN portés par la fiche l'emportent sur les valeurs par défaut")
    void ruleOverridesDefaults() {
        givenServers(server("special", GameServerStatus.ONLINE, port("tcp", 15007, 25565, "192.168.1.200")));

        PortRule rule = onlyRule(resolver.resolve(null, false));

        assertThat(rule.lanPort()).isEqualTo(25565);
        assertThat(rule.lanIp()).isEqualTo("192.168.1.200");
    }

    @Test
    @DisplayName("les règles permanentes sont ouvertes quel que soit l'état des serveurs")
    void staticRulesIgnoreServers() {
        properties.setStaticRules(List.of(staticRule("wireguard", "udp", 51820, null, 51820, "192.168.1.200")));
        givenNoServers();

        PortRule rule = onlyRule(resolver.resolve(null, false));

        assertThat(rule.owner()).isEqualTo("static/wireguard");
        assertThat(rule.open()).isTrue();
        assertThat(rule.lanIp()).isEqualTo("192.168.1.200");
    }

    @Test
    @DisplayName("une règle permanente à enabled=false reste déclarée mais fermée")
    void disabledStaticRuleStaysClosed() {
        PortForwardingProperties.StaticRule rule = staticRule("teamspeak", "tcp", 30033, null, null, null);
        rule.setEnabled(false);
        properties.setStaticRules(List.of(rule));
        givenNoServers();

        assertThat(onlyRule(resolver.resolve(null, false)).open()).isFalse();
    }

    @Test
    @DisplayName("une plage de ports permanente conserve ses deux bornes")
    void keepsPortRange() {
        properties.setStaticRules(List.of(staticRule("voice", "udp", 9987, 9989, 9987, null)));
        givenNoServers();

        PortRule rule = onlyRule(resolver.resolve(null, false));

        assertThat(rule.wanPortStart()).isEqualTo(9987);
        assertThat(rule.wanPortEnd()).isEqualTo(9989);
    }

    @Test
    @DisplayName("un serveur sans port déclaré ne produit aucune règle")
    void serverWithoutPortsProducesNothing() {
        givenServers(server("sans-ports", GameServerStatus.ONLINE));

        PortRuleResolution resolution = resolver.resolve(null, false);

        assertThat(resolution.rules()).isEmpty();
        assertThat(resolution.rejected()).isEmpty();
    }

    // --- fixtures ---------------------------------------------------------

    private void givenServers(GameServer... servers) {
        when(gameServerService.findAll()).thenReturn(List.of(servers));
    }

    private void givenNoServers() {
        when(gameServerService.findAll()).thenReturn(List.of());
    }

    private PortRule onlyRule(PortRuleResolution resolution) {
        assertThat(resolution.rules()).hasSize(1);
        return resolution.rules().values().iterator().next();
    }

    private PortRule byOwner(PortRuleResolution resolution, String owner) {
        return resolution.rules().values().stream()
                .filter(rule -> owner.equals(rule.owner()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("aucune règle pour " + owner));
    }

    private GameServer server(String identifier, GameServerStatus status, GameServerPort... ports) {
        GameServer entity = new GameServer();
        entity.setSlug(identifier);
        entity.setStatus(status);
        entity.setPorts(List.of(ports));
        return entity;
    }

    private GameServerPort port(String proto, Integer wanPort, Integer lanPort, String lanIp) {
        return new GameServerPort(proto, wanPort, lanPort, lanIp);
    }

    private PortForwardingProperties.StaticRule staticRule(String name, String proto, Integer wanStart,
                                                           Integer wanEnd, Integer lanPort, String lanIp) {
        PortForwardingProperties.StaticRule rule = new PortForwardingProperties.StaticRule();
        rule.setName(name);
        rule.setProto(proto);
        rule.setWanPortStart(wanStart);
        rule.setWanPortEnd(wanEnd);
        rule.setLanPort(lanPort);
        rule.setLanIp(lanIp);
        return rule;
    }
}
