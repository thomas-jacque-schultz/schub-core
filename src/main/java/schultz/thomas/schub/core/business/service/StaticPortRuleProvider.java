package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.config.PortForwardingProperties;

import java.util.List;

@FunctionalInterface
public interface StaticPortRuleProvider {

    List<PortForwardingProperties.StaticRule> staticRules();
}
