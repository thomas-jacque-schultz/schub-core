package schultz.thomas.schub.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import schultz.thomas.schub.core.config.PortForwardingProperties;
import schultz.thomas.schub.core.portforwarding.PortForwardingReport;
import schultz.thomas.schub.core.portforwarding.PortForwardingReport.Outcome;
import schultz.thomas.schub.core.portforwarding.PortRule;
import schultz.thomas.schub.core.portforwarding.PortRuleResolution;
import schultz.thomas.schub.core.portforwarding.Protocol;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * L'état voulu est fourni tel quel par un {@link PortRuleResolver} simulé : ces tests
 * ne portent que sur la comparaison avec le routeur et sur ce qui lui est envoyé.
 */
@ExtendWith(MockitoExtension.class)
class PortForwardingServiceTest {

    private static final String LAN_IP = "192.168.1.202";

    @Mock
    private RedirectionRequestService redirectionRequestService;

    @Mock
    private PortRuleResolver resolver;

    private PortForwardingProperties properties;

    private PortForwardingService service;

    @BeforeEach
    void setUp() {
        properties = new PortForwardingProperties();
        properties.setEnabled(true);
        service = new PortForwardingService(properties, redirectionRequestService, resolver);
    }

    @Test
    @DisplayName("intégration coupée : le routeur n'est jamais interrogé")
    void skipsWhenDisabled() {
        properties.setEnabled(false);

        PortForwardingReport report = service.reconcile();

        assertThat(report.applied()).isFalse();
        assertThat(report.skippedReason()).contains("enabled=false");
        verifyNoInteractions(redirectionRequestService, resolver);
    }

    @Test
    @DisplayName("routeur indisponible : le motif de l'adapter est repris tel quel")
    void skipsWhenRouterUnavailable() {
        when(redirectionRequestService.unavailableReason()).thenReturn("aucun jeton d'appairage");

        PortForwardingReport report = service.reconcile();

        assertThat(report.applied()).isFalse();
        assertThat(report.skippedReason()).isEqualTo("aucun jeton d'appairage");
        verify(redirectionRequestService, never()).listRules();
    }

    @Test
    @DisplayName("une règle voulue absente du routeur est créée dans l'état demandé")
    void createsMissingRule() {
        givenDesired(rule(null, "minecraft-ftb", Protocol.TCP, 15007, true));
        givenCurrent();

        PortForwardingReport report = service.reconcile();

        ArgumentCaptor<PortRule> created = ArgumentCaptor.forClass(PortRule.class);
        verify(redirectionRequestService).createRule(created.capture());

        assertThat(created.getValue().owner()).isEqualTo("minecraft-ftb");
        assertThat(created.getValue().open()).isTrue();
        assertThat(report.count(Outcome.CREATED)).isEqualTo(1);
    }

    @Test
    @DisplayName("une règle à fermer est mise à jour en conservant son identifiant routeur")
    void closesRule() {
        givenDesired(rule(null, "palworld-miam", Protocol.UDP, 8211, false));
        givenCurrent(rule("12", "palworld-miam", Protocol.UDP, 8211, true));

        PortForwardingReport report = service.reconcile();

        ArgumentCaptor<PortRule> updated = ArgumentCaptor.forClass(PortRule.class);
        verify(redirectionRequestService).updateRule(updated.capture());

        assertThat(updated.getValue().providerId()).isEqualTo("12");
        assertThat(updated.getValue().open()).isFalse();
        assertThat(report.count(Outcome.CLOSED)).isEqualTo(1);
        verify(redirectionRequestService, never()).createRule(any());
    }

    @Test
    @DisplayName("une règle à ouvrir est mise à jour")
    void opensRule() {
        givenDesired(rule(null, "palworld-miam", Protocol.UDP, 8211, true));
        givenCurrent(rule("12", "palworld-miam", Protocol.UDP, 8211, false));

        assertThat(service.reconcile().count(Outcome.OPENED)).isEqualTo(1);
        verify(redirectionRequestService).updateRule(any());
    }

    @Test
    @DisplayName("une règle déjà dans le bon état n'est pas réécrite")
    void leavesCorrectRuleAlone() {
        PortRule wanted = rule(null, "palworld-miam", Protocol.UDP, 8211, true);
        givenDesired(wanted);
        givenCurrent(rule("12", "palworld-miam", Protocol.UDP, 8211, true));

        PortForwardingReport report = service.reconcile();

        assertThat(report.count(Outcome.UNCHANGED)).isEqualTo(1);
        assertThat(report.hasChanges()).isFalse();
        verify(redirectionRequestService, never()).updateRule(any());
        verify(redirectionRequestService, never()).createRule(any());
    }

    @Test
    @DisplayName("une cible LAN modifiée déclenche un réacheminement")
    void reroutesWhenLanTargetChanges() {
        givenDesired(new PortRule(null, "minecraft-ftb", Protocol.TCP, 15007, 15007, "192.168.1.200", 15007, true));
        givenCurrent(new PortRule("3", "minecraft-ftb", Protocol.TCP, 15007, 15007, LAN_IP, 15007, true));

        PortForwardingReport report = service.reconcile();

        ArgumentCaptor<PortRule> updated = ArgumentCaptor.forClass(PortRule.class);
        verify(redirectionRequestService).updateRule(updated.capture());

        assertThat(updated.getValue().lanIp()).isEqualTo("192.168.1.200");
        assertThat(report.count(Outcome.REROUTED)).isEqualTo(1);
    }

    @Test
    @DisplayName("une redirection manuelle sur le même port est signalée, jamais touchée")
    void neverTouchesManualRule() {
        givenDesired(rule(null, "palworld-miam", Protocol.UDP, 8211, true));
        givenCurrent(new PortRule("7", null, Protocol.UDP, 8211, 8211, "192.168.1.50", 8211, true));

        PortForwardingReport report = service.reconcile();

        assertThat(report.conflicts()).hasSize(1);
        verify(redirectionRequestService, never()).createRule(any());
        verify(redirectionRequestService, never()).updateRule(any());
        verify(redirectionRequestService, never()).deleteRule(any());
    }

    @Test
    @DisplayName("une règle à nous devenue orpheline est fermée, pas supprimée par défaut")
    void closesOrphanByDefault() {
        givenDesired();
        givenCurrent(rule("9", "serveur-supprime", Protocol.TCP, 25565, true));

        PortForwardingReport report = service.reconcile();

        ArgumentCaptor<PortRule> updated = ArgumentCaptor.forClass(PortRule.class);
        verify(redirectionRequestService).updateRule(updated.capture());

        assertThat(updated.getValue().open()).isFalse();
        assertThat(report.count(Outcome.CLOSED)).isEqualTo(1);
        verify(redirectionRequestService, never()).deleteRule(any());
    }

    @Test
    @DisplayName("une orpheline déjà fermée ne provoque aucun appel")
    void ignoresAlreadyClosedOrphan() {
        givenDesired();
        givenCurrent(rule("9", "serveur-supprime", Protocol.TCP, 25565, false));

        PortForwardingReport report = service.reconcile();

        assertThat(report.hasChanges()).isFalse();
        verify(redirectionRequestService, never()).updateRule(any());
    }

    @Test
    @DisplayName("prune-orphans supprime la règle orpheline au lieu de la fermer")
    void deletesOrphanWhenPruneEnabled() {
        properties.setPruneOrphans(true);
        givenDesired();
        givenCurrent(rule("9", "serveur-supprime", Protocol.TCP, 25565, true));

        PortForwardingReport report = service.reconcile();

        verify(redirectionRequestService).deleteRule(any());
        assertThat(report.count(Outcome.DELETED)).isEqualTo(1);
    }

    @Test
    @DisplayName("en dry-run, le routeur est lu mais jamais modifié")
    void dryRunNeverWrites() {
        properties.setDryRun(true);
        givenDesired(rule(null, "minecraft-ftb", Protocol.TCP, 15007, true));
        givenCurrent();

        PortForwardingReport report = service.reconcile();

        assertThat(report.count(Outcome.CREATED)).isEqualTo(1);
        verify(redirectionRequestService, never()).createRule(any());
        verify(redirectionRequestService, never()).updateRule(any());
        verify(redirectionRequestService, never()).deleteRule(any());
    }

    @Test
    @DisplayName("les règles écartées par le resolver sont remontées dans le compte rendu")
    void reportsRejectedRules() {
        when(redirectionRequestService.unavailableReason()).thenReturn(null);
        when(resolver.resolve(null, false))
                .thenReturn(new PortRuleResolution(Map.of(), List.of("jeu-a: port 22 interdit")));
        givenCurrent();

        assertThat(service.reconcile().rejected()).containsExactly("jeu-a: port 22 interdit");
    }

    @Test
    @DisplayName("une panne du routeur est absorbée, jamais propagée à l'appelant")
    void swallowsRouterFailure() {
        when(redirectionRequestService.unavailableReason()).thenReturn(null);
        when(resolver.resolve(null, false)).thenReturn(new PortRuleResolution(Map.of(), List.of()));
        when(redirectionRequestService.listRules()).thenThrow(new IllegalStateException("box injoignable"));

        PortForwardingReport report = service.reconcile();

        assertThat(report.applied()).isFalse();
        assertThat(report.skippedReason()).contains("box injoignable");
    }

    @Test
    @DisplayName("l'override est transmis au resolver")
    void forwardsOverrideToResolver() {
        when(redirectionRequestService.unavailableReason()).thenReturn(null);
        when(resolver.resolve("minecraft-ftb", true))
                .thenReturn(new PortRuleResolution(Map.of(), List.of()));
        givenCurrent();

        service.reconcile("minecraft-ftb", true);

        verify(resolver).resolve("minecraft-ftb", true);
    }

    // --- fixtures ---------------------------------------------------------

    private void givenDesired(PortRule... rules) {
        when(redirectionRequestService.unavailableReason()).thenReturn(null);
        Map<schultz.thomas.schub.core.portforwarding.PortRuleKey, PortRule> indexed =
                java.util.Arrays.stream(rules).collect(Collectors.toMap(PortRule::key, Function.identity(),
                        (first, second) -> first, java.util.LinkedHashMap::new));
        when(resolver.resolve(null, false)).thenReturn(new PortRuleResolution(indexed, List.of()));
    }

    private void givenCurrent(PortRule... rules) {
        when(redirectionRequestService.listRules()).thenReturn(List.of(rules));
    }

    private PortRule rule(String providerId, String owner, Protocol protocol, int wanPort, boolean open) {
        return new PortRule(providerId, owner, protocol, wanPort, wanPort, LAN_IP, wanPort, open);
    }
}
