package schultz.thomas.schub.core.api.dto;

/**
 * Une règle permanente telle que le cœur l'expose.
 *
 * <p>Le contrôleur renvoyait jusqu'ici {@code StaticPortRuleEntity} directement. C'était deux
 * fautes en une : la couche {@code api} exposait un type de {@code data}, contre la règle du
 * §2 bis de la découpe ; et surtout l'entité servait aussi de <em>corps de requête</em>, ce que
 * Sonar signale en {@code java:S4684}. Voir {@link StaticPortRuleRequest} pour le détail du
 * risque.</p>
 */
public record StaticPortRuleDto(
        /** identifiant attribué par la base ; c'est lui qui permet la suppression */
        String id,
        /** libellé humain ; devient {@code static/<name>} comme propriétaire de la règle */
        String name,
        /** "tcp" ou "udp" */
        String proto,
        Integer wanPortStart,
        /** absent = redirection d'un port unique */
        Integer wanPortEnd,
        /** absent = identique au port WAN */
        Integer lanPort,
        /** absent = port-forwarding.default-lan-ip */
        String lanIp,
        /** permet de fermer une règle sans la supprimer */
        boolean enabled
) {
}
