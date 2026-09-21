package schultz.thomas.schub.core.team.business.model;

/**
 * Pourquoi la colonne d'un membre contient ce qu'elle contient — et, le plus souvent, pourquoi
 * elle est vide.
 *
 * <h2>Le piège que cet état évite</h2>
 *
 * <p>Un membre sans maîtrises a deux façons de mal finir : faire échouer toute la requête, ou
 * disparaître de la page. Les deux sont des pertes d'information — dans le premier cas, un seul
 * joueur non lié rend le panneau inutilisable pour les quatre autres ; dans le second, on cherche
 * pendant dix minutes pourquoi il manque quelqu'un dans l'effectif.</p>
 *
 * <p>Le membre est donc <strong>toujours rendu</strong>, avec la raison. C'est ce qui permet à
 * l'écran d'inviter à lier son compte Riot plutôt que d'afficher un trou.</p>
 */
public enum PoolState {

    /** Des maîtrises ont été lues et croisées au catalogue. Le seul état où la liste est pleine. */
    MAITRISES_CONNUES,

    /**
     * Ce membre n'a pas de {@code puuid} : personne n'a encore lié ce Riot ID à un compte, ou le
     * connecteur n'avait pas pu le résoudre à l'ajout.
     *
     * <p>C'est l'état qui appelle une action, et la seule à proposer :
     * {@code PUT /users/me/riot-account} puis {@code POST /teams/claim}.</p>
     */
    COMPTE_RIOT_ABSENT,

    /**
     * Le {@code puuid} est connu mais le connecteur n'a pas rendu ses maîtrises — éteint, sans
     * clé, ou quota épuisé.
     *
     * <p>Rien à faire côté utilisateur : réessayer plus tard. À ne surtout pas confondre avec
     * {@link #AUCUNE_MAITRISE}, qui est une réponse, pas une absence de réponse.</p>
     */
    MAITRISES_INDISPONIBLES,

    /**
     * Le catalogue des champions n'a pas pu être lu, donc <strong>aucun</strong> champion n'est
     * rendu pour personne.
     *
     * <p>C'est la contrepartie de la règle « pas de patch, pas de champion » : sans le catalogue
     * on n'a ni nom, ni icône, ni version — et une donnée de champion sans sa version est fausse
     * dans trois mois sans qu'on le voie. Mieux vaut ne rien servir et le dire.</p>
     */
    CATALOGUE_INDISPONIBLE,

    /**
     * Le connecteur a répondu, et ce joueur n'a aucune maîtrise. Un compte neuf, essentiellement.
     * C'est une information, pas une panne.
     */
    AUCUNE_MAITRISE
}
