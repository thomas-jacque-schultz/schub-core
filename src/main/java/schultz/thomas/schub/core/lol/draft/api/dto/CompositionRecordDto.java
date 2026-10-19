package schultz.thomas.schub.core.lol.draft.api.dto;

import java.time.Instant;
import java.util.List;

// Bilan d'une composition préparée (Schub#107) : les parties où nos cinq champions étaient dans notre camp.
public record CompositionRecordDto(String compositionId, long games, long wins, boolean lowConfidence,
                                   List<Game> matches) {

    public record Game(String matchId, Instant startedAt, boolean win, long durationSeconds) {
    }
}
