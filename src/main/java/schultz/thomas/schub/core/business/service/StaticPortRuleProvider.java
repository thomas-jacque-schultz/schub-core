package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.config.PortForwardingProperties;

import java.util.List;

/**
 * Source des règles permanentes pour le réconciliateur.
 *
 * <p>Existe pour que {@link PortRuleResolver} reste une fonction pure : il consomme une liste,
 * sans savoir si elle vient d'un fichier de configuration ou de la base. La production la lit
 * en base ; les tests la fournissent en dur.</p>
 */
@FunctionalInterface
public interface StaticPortRuleProvider {

    List<PortForwardingProperties.StaticRule> staticRules();
}
