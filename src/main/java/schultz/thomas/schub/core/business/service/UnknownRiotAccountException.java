package schultz.thomas.schub.core.business.service;

/** Riot a répondu qu'aucun compte ne porte ce Riot ID. */
public class UnknownRiotAccountException extends RuntimeException {

    public UnknownRiotAccountException(String riotId) {
        super("Aucun compte Riot ne porte « " + riotId + " ». Vérifiez le pseudo et le TAG.");
    }
}
