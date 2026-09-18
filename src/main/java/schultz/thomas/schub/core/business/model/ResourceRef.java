package schultz.thomas.schub.core.business.model;

/**
 * Désigne la ressource sur laquelle une permission est évaluée, ou {@code null} pour une
 * question globale (« a-t-il le droit de créer un serveur ? »).
 *
 * @param type le type de ressource
 * @param id   son identifiant <em>dans le vocabulaire du domaine</em> : pour un
 *             {@link ResourceType#GAME_SERVER}, c'est le slug — le seul identifiant qu'un
 *             consommateur connaisse (migration §4), et celui qui est stable.
 */
public record ResourceRef(ResourceType type, String id) {

    public static ResourceRef gameServer(String slug) {
        return slug == null ? null : new ResourceRef(ResourceType.GAME_SERVER, slug);
    }
}
