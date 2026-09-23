package schultz.thomas.schub.core.business.service;

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
