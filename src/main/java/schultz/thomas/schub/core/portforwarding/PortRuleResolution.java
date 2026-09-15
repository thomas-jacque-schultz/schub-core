package schultz.thomas.schub.core.portforwarding;

import java.util.List;
import java.util.Map;

/**
 * Résultat du calcul de l'état voulu.
 *
 * @param rules    les règles retenues, indexées par leur identité WAN
 * @param rejected les règles écartées, avec le motif (port interdit, doublon, configuration invalide)
 */
public record PortRuleResolution(Map<PortRuleKey, PortRule> rules, List<String> rejected) {
}
