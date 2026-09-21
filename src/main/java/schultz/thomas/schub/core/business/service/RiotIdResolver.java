package schultz.thomas.schub.core.business.service;

import java.util.Optional;

/**
 * {@code Pseudo#TAG} → {@code puuid}.
 *
 * <h2>Pourquoi elle a quitté le paquet {@code team} au lot D.3</h2>
 *
 * <p>Elle y était née, parce que le lot D.4 était son seul appelant. Le lot D.3 lui en donne un
 * second, et il est dans le domaine de l'<em>identité</em> : c'est le {@code User} qui porte le
 * lien vers le compte Riot (plan §D.2). L'y laisser aurait obligé
 * {@link RiotAccountService} à importer une classe de {@code team} — soit exactement la dérive
 * que le test de la PR du lot D.4 cherche, et dans le mauvais sens : {@code team} lit
 * l'identité, jamais l'inverse.</p>
 *
 * <p>Sa place est ici de toute façon : ce n'est pas une règle d'équipe, c'est l'adaptateur d'une
 * route du connecteur Riot, au même titre que {@code ContainerRequestService} l'est pour
 * Portainer. Le paquet {@code team} continue de l'utiliser — cette direction-là est permise.</p>
 *
 * <h2>La résolution est au mieux, jamais bloquante</h2>
 *
 * <p>Un échec rend {@link Optional#empty()} plutôt que de lever : connecteur éteint, clé de
 * développement expirée — elle l'est toutes les 24 h —, Riot ID inexistant, dans les trois cas la
 * bonne réponse est « je ne sais pas ». Ce que fait l'appelant de ce « je ne sais pas » lui
 * appartient : le lot D.4 ajoute le membre sans {@code puuid}, le lot D.3 enregistre le Riot ID
 * en attente de résolution. Aucun des deux ne fait dépendre une fonctionnalité du domaine de la
 * disponibilité d'une API tierce.</p>
 */
public interface RiotIdResolver {

    RiotIdResolution resolve(String gameName, String tagLine);

    default Optional<String> resolvePuuid(String gameName, String tagLine) {
        return Optional.ofNullable(resolve(gameName, tagLine).puuid());
    }
}
