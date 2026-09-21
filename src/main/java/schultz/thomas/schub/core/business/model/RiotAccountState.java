package schultz.thomas.schub.core.business.model;

/**
 * Où en est le lien entre un compte Schub et un compte Riot.
 *
 * <p><strong>Trois états et non un booléen</strong>, parce qu'il en existe bel et bien trois. Le
 * {@code puuid} est la seule clé stable côté Riot, mais le résoudre demande le connecteur, qui
 * peut être éteint ou sans clé — celle de développement expire toutes les 24 h. Un Riot ID
 * déclaré sans {@code puuid} n'est donc ni « lié », ni « absent » : c'est une déclaration qui
 * attend d'être résolue, et l'écran doit pouvoir le dire au lieu de laisser croire que tout va
 * bien.</p>
 *
 * <p>Écraser ce troisième état en {@code linked = true} donnerait une équipe dont les panneaux
 * restent vides sans que personne ne comprenne pourquoi ; en {@code linked = false}, une saisie
 * perdue et à refaire. Les deux sont des mensonges commodes.</p>
 */
public enum RiotAccountState {

    /** Aucun Riot ID déclaré. C'est l'état de tout compte à sa création. */
    ABSENT,

    /**
     * Riot ID déclaré <strong>et</strong> {@code puuid} connu. Le seul état où les maîtrises, les
     * classements et l'historique sont accessibles : tout le reste du chantier D part du
     * {@code puuid}.
     */
    RESOLU,

    /**
     * Riot ID déclaré, {@code puuid} inconnu — le connecteur n'a pas répondu, ou ce Riot ID
     * n'existe pas.
     *
     * <p>La saisie est conservée : elle suffit à revendiquer une place d'effectif libre
     * ({@code POST /teams/claim} compare aussi les Riot ID), et renvoyer le même Riot ID sur la
     * route de liaison relance la résolution. Ce qu'elle ne permet pas, c'est la moindre donnée
     * de jeu.</p>
     */
    EN_ATTENTE_DE_RESOLUTION
}
