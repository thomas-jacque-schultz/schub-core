package schultz.thomas.schub.core.lol.riot.api.dto;

// accountsToResolve : puuid des comptes liés et des places d'équipe, résolus de nouveau en arrière-plan.
public record RiotDataInvalidationDto(long riotDocuments, long findings, int accountsToResolve) {
}
