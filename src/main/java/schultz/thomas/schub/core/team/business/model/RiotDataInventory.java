package schultz.thomas.schub.core.team.business.model;

// Ce qu'une invalidation emporterait (documents Riot, constats) et ce qu'elle garde.
public record RiotDataInventory(
        boolean available,
        long riotDocuments,
        long findings,
        long linkedAccounts,
        long teamSlots,
        long teams) {
}
