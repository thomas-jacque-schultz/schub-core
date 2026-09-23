package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "servers")
public class GameServer {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug;

    private Integer deploymentId;

    private String name;
    private String urlConnection;
    private Game game;
    private Integer playersMax;
    private String installation;
    private String version;
    private String description;

    private List<GameServerPort> ports = new ArrayList<>();

    // null = jamais observé ; force la notification au premier passage.
    private GameServerStatus status;

    private Instant lastStatusCheckAt;

    private Instant lastStatusChangeAt;

    private List<GameServerStatusHistoryEntry> statusHistory = new ArrayList<>();
}
