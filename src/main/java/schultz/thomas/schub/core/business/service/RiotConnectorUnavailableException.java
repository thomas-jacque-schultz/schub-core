package schultz.thomas.schub.core.business.service;

/**
 * Le connecteur Riot n'a pas répondu à une question qu'on lui a explicitement posée.
 *
 * <p>Partout ailleurs, son silence est avalé : un profil s'affiche, une équipe se constitue, un
 * Riot ID se déclare sans lui. Ici non — quelqu'un a demandé « ce compte existe-t-il ? » et
 * répondre « non » à sa place serait faux.</p>
 */
public class RiotConnectorUnavailableException extends RuntimeException {

    public RiotConnectorUnavailableException() {
        super("La vérification auprès de Riot est indisponible. Réessayez dans un instant.");
    }
}
