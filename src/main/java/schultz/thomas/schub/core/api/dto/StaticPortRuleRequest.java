package schultz.thomas.schub.core.api.dto;

/**
 * Ce qu'un appelant a le droit de dire en créant une règle permanente.
 *
 * <p><strong>Il n'y a pas de champ {@code id}, et c'est tout l'objet de ce type.</strong> Le
 * contrôleur liait auparavant le corps de la requête sur {@code StaticPortRuleEntity}, l'entité
 * persistée elle-même. Spring remplit alors <em>tous</em> les champs présents dans le JSON, y
 * compris ceux que l'appelant n'était pas censé choisir : un {@code POST} portant un {@code id}
 * existant n'aurait pas créé une règle, il en aurait <em>écrasé</em> une autre — celle d'un port
 * que personne n'avait demandé à rouvrir.</p>
 *
 * <p>C'est le défaut d'affectation massive, {@code java:S4684}, que SonarCloud classe en
 * vulnérabilité critique. La parade n'est pas de filtrer l'{@code id} à la main dans le service
 * — un filtre s'oublie au prochain champ ajouté — mais de <strong>ne pas offrir le champ</strong> :
 * ce que le type ne porte pas ne peut pas être affecté.</p>
 */
public record StaticPortRuleRequest(
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
        boolean enabled
) {
}
