package schultz.thomas.schub.core.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Début d'un segment d'état. La fin se déduit de l'entrée suivante. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameServerStatusHistoryEntry {

    private GameServerStatus status;

    private Instant startedAt;
}
