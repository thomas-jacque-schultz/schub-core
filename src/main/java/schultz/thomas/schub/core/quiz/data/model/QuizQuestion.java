package schultz.thomas.schub.core.quiz.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// La bonne réponse est la première des quatre : le quiz tire l'ordre à chaque partie.
@Data
@Document("quiz_questions")
public class QuizQuestion {

    public enum Theme { PROS, MONDE }

    public record Wording(String enonce, List<String> choix, String explication) {
    }

    @Id
    private String id;

    private Theme theme;
    private String date;
    private String source;
    private Wording fr;
    private Wording en;
    private boolean active = true;

    private String updatedBy;
    private Instant updatedAt;
}
