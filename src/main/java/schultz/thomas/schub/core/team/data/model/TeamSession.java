package schultz.thomas.schub.core.team.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// Index posés par V026. Une session de jeu de l'équipe (Schub#103). missed : un créneau planifié sans partie, compté au récap mensuel.
@Document(collection = "team_sessions")
public record TeamSession(
        @Id String id,
        String teamId,
        LocalDate day,
        Instant startedAt,
        Instant endedAt,
        List<String> matchIds,
        List<String> memberIds,
        long games,
        long wins,
        boolean planned,
        String slotId,
        boolean missed,
        Instant detectedAt
) {
}
