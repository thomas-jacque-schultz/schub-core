package schultz.thomas.schub.core.business.service;

public class RiotConnectorBusyException extends RuntimeException {

    public RiotConnectorBusyException() {
        super("La vérification auprès de Riot est momentanément saturée. Réessayez tout de suite.");
    }
}
