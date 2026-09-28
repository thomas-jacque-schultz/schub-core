package schultz.thomas.schub.core.api.controller;

// X-Actor-Id (identifiant interne de l'utilisateur) est cru sur parole : X-Internal-Secret garde la frontière du maillage.
public final class CoreHeaders {

    public static final String ACTOR_ID = "X-Actor-Id";

    // Hachage salé du jour, posé par le BFF sur la recherche publique : compte des personnes, n'en reconnaît aucune.
    public static final String VISITOR = "X-Visitor";

    private CoreHeaders() {
    }
}
