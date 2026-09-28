package schultz.thomas.schub.core.team.api.dto;

// Ce qu'une invalidation emporterait (documents Riot, constats) et ce qu'elle garde.
public record RiotDataInventoryDto(
        boolean available,
        long riotDocuments,
        long findings,
        long linkedAccounts,
        long teamSlots,
        long teams,
        long reviews) {
}
