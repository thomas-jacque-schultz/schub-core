package schultz.thomas.schub.core.business.model;

/**
 * Désigne la ressource sur laquelle une permission est évaluée, ou {@code null} pour une
 * question globale (« a-t-il le droit de créer une équipe ? »).
 *
 * @param type le type de ressource
 * @param id   son identifiant <em>dans le vocabulaire du domaine</em>
 */
public record ResourceRef(ResourceType type, String id) {

    public static ResourceRef team(String id) {
        return id == null ? null : new ResourceRef(ResourceType.TEAM, id);
    }
}
