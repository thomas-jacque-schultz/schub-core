package schultz.thomas.schub.core.business.service;

/**
 * Quatre issues, pas deux. Un Optional vide confondait « ce compte n'existe pas » avec « le
 * connecteur est muet » : le premier doit être refusé, le second accepté en attente.
 *
 * <p>{@code BUSY} a été détaché d'{@code UNAVAILABLE} le 21-09 : le connecteur qui refuse un
 * créneau de quota répond, et le lien se fera à la seconde d'après. Le confondre avec une panne
 * fait dire à l'utilisateur de revenir plus tard alors qu'il lui suffit de réessayer.</p>
 */
public record RiotIdResolution(Outcome outcome, String puuid) {

    public enum Outcome { RESOLVED, NOT_FOUND, BUSY, UNAVAILABLE }

    public static RiotIdResolution resolved(String puuid) {
        return new RiotIdResolution(Outcome.RESOLVED, puuid);
    }

    public static RiotIdResolution notFound() {
        return new RiotIdResolution(Outcome.NOT_FOUND, null);
    }

    public static RiotIdResolution busy() {
        return new RiotIdResolution(Outcome.BUSY, null);
    }

    public static RiotIdResolution unavailable() {
        return new RiotIdResolution(Outcome.UNAVAILABLE, null);
    }

    public boolean isNotFound() {
        return outcome == Outcome.NOT_FOUND;
    }

    public boolean isBusy() {
        return outcome == Outcome.BUSY;
    }
}
