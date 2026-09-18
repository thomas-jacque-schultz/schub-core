package schultz.thomas.schub.core.api.controller;

/**
 * Les en-têtes que le cœur lit sur ses appels entrants.
 *
 * <p>{@code X-Actor-Id} porte l'<strong>identifiant Discord</strong> de la personne au nom de qui
 * l'appel est fait. Le cœur le croit sur parole, et c'est assumé : {@code X-Internal-Secret}
 * garde la frontière du maillage, donc seule une brique Schub peut affirmer quoi que ce soit
 * (plan §A.2). Le jour où ce secret fuite, l'usurpation d'acteur est le moindre des problèmes.</p>
 */
public final class CoreHeaders {

    public static final String ACTOR_ID = "X-Actor-Id";

    private CoreHeaders() {
    }
}
