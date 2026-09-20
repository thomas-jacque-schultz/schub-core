package schultz.thomas.schub.core.business.model;

/**
 * Le type d'une ressource sur laquelle une permission peut être évaluée.
 *
 * <p>Générique dès maintenant, et pas par goût de l'abstraction : le chantier D ajoutera
 * {@code TEAM}, dont la règle est la même — appartenir à une ressource donne des droits sur
 * <em>cette</em> ressource. Écrire l'évaluateur autour d'un {@code slug} de serveur aurait
 * imposé de le réécrire au lot D.4 (plan §A.1).</p>
 */
public enum ResourceType {

    /** L'identifiant porté par la référence est le <strong>slug</strong>, pas l'id Mongo. */
    GAME_SERVER,

    /**
     * Une équipe. L'identifiant porté par la référence est son <strong>id</strong> : une équipe
     * n'a pas de slug, elle n'est jamais désignée par son nom — deux équipes peuvent s'appeler
     * pareil, et un nom se renomme.
     */
    TEAM
}
