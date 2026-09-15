package schultz.thomas.schub.core.service;

import schultz.thomas.schub.core.portforwarding.PortRule;

import java.util.List;

/**
 * Accès aux redirections de ports du routeur, exprimé dans le vocabulaire du domaine.
 *
 * <p>Même rôle que {@link ContainerRequestService} vis-à-vis de Portainer : le réconciliateur
 * ne connaît que cette interface, l'implémentation encapsule l'API du routeur. Changer de box
 * revient à écrire une seconde implémentation et à basculer le {@code @Qualifier}.</p>
 */
public interface RedirectionRequestService {

    /**
     * Motif empêchant d'utiliser le routeur (non appairé, non configuré), ou {@code null}
     * s'il est utilisable. Permet au réconciliateur de se taire proprement sans connaître
     * les conditions propres à chaque routeur.
     */
    String unavailableReason();

    /** Toutes les redirections du routeur, les manuelles comprises (leur {@code owner} est null). */
    List<PortRule> listRules();

    void createRule(PortRule rule);

    /** Met à jour la règle désignée par {@code rule.providerId()}. */
    void updateRule(PortRule rule);

    /** Supprime la règle désignée par {@code rule.providerId()}. */
    void deleteRule(PortRule rule);
}
