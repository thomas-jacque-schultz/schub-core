package schultz.thomas.schub.core.business.model;

/**
 * Le type d'une ressource sur laquelle une permission peut être évaluée.
 *
 * <p>Un serveur de jeu n'en est pas une : le pilotage d'un serveur vient du rôle et de lui seul
 * depuis le retrait des administrateurs par serveur. La mécanique, elle, reste — c'est elle qui
 * porte les droits d'équipe.</p>
 */
public enum ResourceType {

    /**
     * Une équipe. L'identifiant porté par la référence est son <strong>id</strong> : une équipe
     * n'a pas de slug, elle n'est jamais désignée par son nom — deux équipes peuvent s'appeler
     * pareil, et un nom se renomme.
     */
    TEAM
}
