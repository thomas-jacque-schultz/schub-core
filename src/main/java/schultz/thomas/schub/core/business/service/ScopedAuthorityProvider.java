package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.data.model.User;

import java.util.Set;

/**
 * Ce qu'une ressource donne à qui lui appartient — <strong>déclaré par le domaine qui possède
 * cette ressource</strong>, pas par l'évaluateur.
 *
 * <p>C'est l'élargissement demandé par le plan §A.1 plutôt qu'une seconde copie de
 * {@link PermissionEvaluator} : la règle « appartenir à une ressource donne des droits sur
 * <em>cette</em> ressource » est la même pour un serveur et pour une équipe, mais la façon de
 * répondre ne l'est pas — l'un lit {@code admins}, l'autre lit un effectif.</p>
 *
 * <p><strong>Ce que ça achète concrètement</strong> : l'évaluateur n'a besoin d'aucun dépôt du
 * paquet {@code …core.team}, ce que l'interdit n°1 du plan §D.2 exige. Le jour où ce domaine
 * devient un service, son implémentation de cette interface devient un appel HTTP, et
 * l'évaluateur n'est pas rouvert.</p>
 */
public interface ScopedAuthorityProvider {

    /** Le type de ressource auquel ce fournisseur sait répondre. */
    ResourceType resourceType();

    /**
     * Ce que cet acteur gagne sur <em>cette</em> ressource, en plus de ce que son rôle lui donne
     * partout. Un ensemble vide est la réponse normale pour qui n'a rien à y voir.
     *
     * @param resourceId l'identifiant dans le vocabulaire du domaine propriétaire
     */
    Set<Permission> grantedTo(User actor, String resourceId);
}
