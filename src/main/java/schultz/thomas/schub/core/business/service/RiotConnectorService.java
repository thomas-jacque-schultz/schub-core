package schultz.thomas.schub.core.business.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Ce que le cœur demande au connecteur Riot en plus de la résolution d'un Riot ID.
 *
 * <p>Même règle que {@link RiotIdResolver} : <strong>tout échec est avalé</strong>. Connecteur
 * éteint, clé expirée, Mongo du connecteur indisponible — la réponse est « je ne sais pas », et
 * l'appelant décide. Aucune fonctionnalité du domaine ne dépend de la disponibilité du
 * connecteur : c'est ce qui fait qu'un profil s'affiche quand la collecte est à l'arrêt.</p>
 */
public interface RiotConnectorService {

    /** Où en est la collecte pour ce {@code puuid}, ou vide si le connecteur n'a pas répondu. */
    Optional<PlayerIngest> ingestOf(String puuid);

    /**
     * Demande la collecte de l'historique de ce {@code puuid}.
     *
     * @return {@code true} si la demande a été acceptée. {@code false} n'est pas une erreur :
     *         la collecte sera relancée à la prochaine occasion.
     */
    boolean requestIngest(String puuid);

    /** Les comptes connus de nos participations qui ressemblent à cette saisie. Jamais nul. */
    List<KnownPlayer> search(String query, int limit);

    record PlayerIngest(long pending, long running, Instant estimatedReadyAt) {
    }

    /**
     * @param observedAt date de l'observation qui a écrit cette identité — la partie où on a
     *                   croisé ce joueur, ou l'appel qui l'a fait confirmer par Riot.
     * @param source     PARTICIPATION ou RESOLUTION. Servi en chaîne : le connecteur reste libre
     *                   d'en ajouter une sans casser le cœur.
     */
    record KnownPlayer(String puuid, String gameName, String tagLine, String riotId,
                       long matchCount, List<PositionPlayed> positions, Instant lastPlayedAt,
                       Instant observedAt, String source) {
    }

    record PositionPlayed(String position, long matches) {
    }
}
