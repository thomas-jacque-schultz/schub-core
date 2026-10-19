package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.business.model.PortRule;

import java.util.List;

public interface RedirectionRequestService {

    String unavailableReason();

    List<PortRule> listRules();

    void createRule(PortRule rule);

    void updateRule(PortRule rule);

    void deleteRule(PortRule rule);
}
