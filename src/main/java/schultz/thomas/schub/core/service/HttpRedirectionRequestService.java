package schultz.thomas.schub.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import schultz.thomas.schub.core.portforwarding.PortRule;
import schultz.thomas.schub.core.portforwarding.RouterStatus;

import java.util.List;

/**
 * Implémentation de {@link RedirectionRequestService} déléguant à {@code schub-connector-freebox}.
 *
 * <p>Depuis la phase 1, ce service ne parle plus à la Freebox : il parle à un connecteur qui,
 * lui, sait le faire. Le vocabulaire échangé reste {@link PortRule} de bout en bout — aucune
 * notion propre à la Freebox ne traverse plus cette frontière, et changer de routeur ne
 * demanderait pas une ligne ici.</p>
 *
 * <p>Le réconciliateur est inchangé : il ne connaît que l'interface, et ignore que
 * l'implémentation est passée d'un appel direct à un saut réseau.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpRedirectionRequestService implements RedirectionRequestService {

    @Qualifier("connectorFreeboxRestClient")
    private final RestClient restClient;

    /**
     * Interroge le connecteur ; un connecteur injoignable est un motif d'indisponibilité
     * comme un autre, pas une erreur à propager. Le réconciliateur se tait et repassera.
     */
    @Override
    public String unavailableReason() {
        try {
            RouterStatus status = restClient.get()
                    .uri("/router/status")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RouterStatus.class);

            return status == null ? "réponse vide du connecteur Freebox" : status.unavailableReason();
        } catch (RestClientException e) {
            log.debug("Connecteur Freebox injoignable", e);
            return "connecteur Freebox injoignable: " + e.getMessage();
        }
    }

    @Override
    public List<PortRule> listRules() {
        List<PortRule> rules = restClient.get()
                .uri("/redirections")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return rules == null ? List.of() : rules;
    }

    @Override
    public void createRule(PortRule rule) {
        restClient.post()
                .uri("/redirections")
                .contentType(MediaType.APPLICATION_JSON)
                .body(rule)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void updateRule(PortRule rule) {
        restClient.put()
                .uri("/redirections/{id}", requireProviderId(rule))
                .contentType(MediaType.APPLICATION_JSON)
                .body(rule)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void deleteRule(PortRule rule) {
        restClient.delete()
                .uri("/redirections/{id}", requireProviderId(rule))
                .retrieve()
                .toBodilessEntity();
    }

    /** Une règle sans identifiant n'existe pas encore sur le routeur : rien à modifier. */
    private String requireProviderId(PortRule rule) {
        if (rule.providerId() == null) {
            throw new IllegalArgumentException("Règle sans identifiant de routeur: " + rule.describe());
        }
        return rule.providerId();
    }
}
