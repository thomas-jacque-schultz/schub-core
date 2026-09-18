package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * La projection « membre » d'un serveur : ce que voit tout compte connecté.
 *
 * <p>Trois niveaux existent, et il en faut trois (décision n°10 du 18-09) :</p>
 * <ul>
 *   <li><em>public</em> — {@link PublicServerStatusDto}, sans compte : nom, jeu, statut ;</li>
 *   <li><em>membre</em> — celle-ci, avec {@code SERVER_VIEW} : de quoi rejoindre et suivre ;</li>
 *   <li><em>infra</em> — {@link GameServerDto}, avec {@code SERVER_INFRA_VIEW} : en plus
 *       {@code deploymentId}, les ports et les administrateurs.</li>
 * </ul>
 *
 * <p>La différence n'est pas cosmétique : « tout le monde peut se connecter » veut dire qu'un
 * inconnu obtient le niveau membre. Lui servir la liste des ports ouverts sur la box serait lui
 * offrir une cartographie de l'installation.</p>
 */
public record GameServerMemberDto(
        String id,
        String slug,
        String name,
        String urlConnection,
        String game,
        String gameLabel,
        String gameIconUrl,
        Integer playersMax,
        String installation,
        String version,
        String description,
        String status,
        Instant lastStatusCheckAt,
        Instant lastStatusChangeAt,
        List<GameServerStatusHistoryEntryDto> statusHistory
) {
}
