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
        List<GameServerStatusHistoryEntryDto> statusHistory,
        /**
         * L'acteur de la requête figure-t-il dans les {@code admins} de <em>ce</em> serveur ?
         *
         * <p>Cette projection ne porte pas — et ne doit pas porter — la liste des
         * administrateurs : nommer les administrateurs, c'est décrire l'installation, donc
         * c'est derrière {@code SERVER_INFRA_VIEW}. Mais un compte qui n'a pas cette permission
         * peut très bien être administrateur d'un serveur : c'est même le cas nominal de la
         * décision n°11. Ce booléen est ce qui le lui dit, sans lui dire qui d'autre l'est.</p>
         */
        boolean viewerIsAdmin
) {
}
