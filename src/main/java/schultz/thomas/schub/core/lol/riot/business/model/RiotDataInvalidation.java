package schultz.thomas.schub.core.lol.riot.business.model;

// accountsToResolve : puuid des comptes liés et des places d'équipe, résolus de nouveau en arrière-plan.
public record RiotDataInvalidation(long riotDocuments, long findings, int accountsToResolve) {
}
