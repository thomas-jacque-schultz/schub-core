package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Un serveur que des joueurs rejoignent. L'entité centrale du domaine.
 *
 * <p>À distinguer du <em>Deployment</em> — la stack qui le réalise concrètement. Un GameServer
 * est une intention durable ; son déploiement est un moyen, remplaçable.</p>
 */
@Data
@Document(collection = "servers")
public class GameServer {

    /** Identifiant Mongo, jamais exposé dans les URLs. */
    @Id
    private String id;

    /**
     * Identifiant humain, stable, utilisé dans les URLs, comme propriétaire des règles de ports
     * et comme argument des commandes Discord. Anciennement {@code identifier}.
     */
    @Indexed(unique = true)
    private String slug;

    /**
     * Le déploiement qui réalise ce serveur — l'identifiant de stack côté connecteur Portainer.
     * Anciennement {@code portainerStackId} : le cœur ne nomme plus la marque de l'outil.
     */
    private Integer deploymentId;

    private String name;
    private String urlConnection;
    private Game game;
    private Integer playersMax;
    private String installation;
    private String version;
    private String description;

    /** Ports à ouvrir tant que ce serveur tourne ; vide = aucune redirection pilotée. */
    private List<GameServerPort> ports = new ArrayList<>();

    /** null = jamais observé ; force la notification au premier passage de la boucle. */
    private GameServerStatus status;

    /** Mis à jour à chaque observation, qu'il y ait changement ou non. */
    private Instant lastStatusCheckAt;

    /** Début de l'état courant. */
    private Instant lastStatusChangeAt;

    /** Historique chronologique ; chaque entrée marque le début d'un segment d'état. */
    private List<GameServerStatusHistoryEntry> statusHistory = new ArrayList<>();
}
