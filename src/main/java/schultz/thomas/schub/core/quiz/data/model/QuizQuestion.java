package schultz.thomas.schub.core.quiz.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// La bonne réponse est la première des quatre : le quiz tire l'ordre à chaque partie. Les images ne dépendent pas
// de la langue ; imagesChoix suit l'ordre des choix (une rangée d'icônes par choix).
@Data
@Document("quiz_questions")
public class QuizQuestion {

    public enum Theme { PROS, MONDE, OBJETS, HISTOIRE, SKINS }

    public record Wording(String enonce, List<String> choix, String explication) {
    }

    @Id
    private String id;

    private Theme theme;
    private String date;
    private String source;
    private String image;
    private List<List<String>> imagesChoix;
    private Wording fr;
    private Wording en;
    private boolean active = true;

    private String updatedBy;
    private Instant updatedAt;
}
