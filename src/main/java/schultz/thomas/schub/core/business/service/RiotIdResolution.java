package schultz.thomas.schub.core.business.service;

// gameName et tagLine : la casse canonique rendue par Riot, absente hors résolution.
public record RiotIdResolution(Outcome outcome, String puuid, String gameName, String tagLine) {

    public enum Outcome { RESOLVED, NOT_FOUND, BUSY, UNAVAILABLE }

    public static RiotIdResolution resolved(String puuid) {
        return new RiotIdResolution(Outcome.RESOLVED, puuid, null, null);
    }

    public static RiotIdResolution resolved(String puuid, String gameName, String tagLine) {
        return new RiotIdResolution(Outcome.RESOLVED, puuid, gameName, tagLine);
    }

    public static RiotIdResolution notFound() {
        return new RiotIdResolution(Outcome.NOT_FOUND, null, null, null);
    }

    public static RiotIdResolution busy() {
        return new RiotIdResolution(Outcome.BUSY, null, null, null);
    }

    public static RiotIdResolution unavailable() {
        return new RiotIdResolution(Outcome.UNAVAILABLE, null, null, null);
    }

    public boolean isNotFound() {
        return outcome == Outcome.NOT_FOUND;
    }

    public boolean isBusy() {
        return outcome == Outcome.BUSY;
    }
}
