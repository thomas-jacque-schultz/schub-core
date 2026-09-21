package schultz.thomas.schub.core.business.service;

/**
 * Trois issues, pas deux. Un Optional vide confondait « ce compte n'existe pas » avec « le
 * connecteur est muet » : le premier doit être refusé, le second accepté en attente.
 */
public record RiotIdResolution(Outcome outcome, String puuid) {

    public enum Outcome { RESOLVED, NOT_FOUND, UNAVAILABLE }

    public static RiotIdResolution resolved(String puuid) {
        return new RiotIdResolution(Outcome.RESOLVED, puuid);
    }

    public static RiotIdResolution notFound() {
        return new RiotIdResolution(Outcome.NOT_FOUND, null);
    }

    public static RiotIdResolution unavailable() {
        return new RiotIdResolution(Outcome.UNAVAILABLE, null);
    }

    public boolean isNotFound() {
        return outcome == Outcome.NOT_FOUND;
    }
}
