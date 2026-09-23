package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "team_game_reviews")
@CompoundIndex(name = "revue_unique",
        def = "{'matchId': 1, 'subjectMemberId': 1, 'authorUserId': 1}", unique = true)
public class GameReview {

    @Id
    private String id;

    @Indexed
    private String teamId;

    private String matchId;

    private String subjectMemberId;

    private String authorUserId;

    private String content;

    private Instant createdAt;
    private Instant updatedAt;
}
