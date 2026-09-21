package schultz.thomas.schub.core.business.service;

/**
 * Le connecteur Riot est occupé : il a refusé un créneau de quota, pas répondu à côté.
 *
 * <p>Distincte de {@link RiotConnectorUnavailableException} parce que la conduite l'est :
 * ici, réessayer tout de suite a des chances d'aboutir.</p>
 */
public class RiotConnectorBusyException extends RuntimeException {

    public RiotConnectorBusyException() {
        super("La vérification auprès de Riot est momentanément saturée. Réessayez tout de suite.");
    }
}
