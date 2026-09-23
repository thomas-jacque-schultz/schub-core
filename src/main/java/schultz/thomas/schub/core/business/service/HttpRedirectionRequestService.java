package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.api.dto.PortRule;
import schultz.thomas.schub.core.business.model.RouterStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpRedirectionRequestService implements RedirectionRequestService {

    @Qualifier("connectorFreeboxRestClient")
    private final RestClient restClient;

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

    private String requireProviderId(PortRule rule) {
        if (rule.providerId() == null) {
            throw new IllegalArgumentException("Règle sans identifiant de routeur: " + rule.describe());
        }
        return rule.providerId();
    }
}
