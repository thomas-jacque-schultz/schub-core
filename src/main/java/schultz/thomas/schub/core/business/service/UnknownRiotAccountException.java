package schultz.thomas.schub.core.business.service;

public class UnknownRiotAccountException extends RuntimeException {

    public UnknownRiotAccountException(String riotId) {
        super("Aucun compte Riot ne porte « " + riotId + " ». Vérifiez le pseudo et le TAG.");
    }
}
