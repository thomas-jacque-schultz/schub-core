package schultz.thomas.schub.core.business.service;

public class RiotConnectorUnavailableException extends RuntimeException {

    public RiotConnectorUnavailableException() {
        super("La vérification auprès de Riot est indisponible. Réessayez dans un instant.");
    }
}
