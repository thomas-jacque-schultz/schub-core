package schultz.thomas.schub.core.team.api.dto;

// accountsToResolve : puuid des comptes liés et des places d'équipe, résolus de nouveau en arrière-plan.
public record RiotDataInvalidationDto(long riotDocuments, long findings, int accountsToResolve) {
}
