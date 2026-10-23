package schultz.thomas.schub.core.data.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

// Les questions fabriquées depuis Data Dragon et Univers (Schub#143), copie de src/games/quiz/fabriquees.json du front.
// Une question déjà en base n'est pas touchée : l'OWNER a pu la corriger.
@ChangeUnit(id = "quiz-fabriquees", order = "029", author = "schub")
public class V029_QuizFabriquees {

    record Libelles(String enonce, List<String> choix, String explication) {
    }

    record Entree(String id, String theme, String date, String source, String image, List<List<String>> imagesChoix,
                  Libelles fr, Libelles en) {
    }

    @Execution
    public void execution(MongoTemplate mongo) {
        Instant maintenant = Instant.now();
        for (Entree entree : lis()) {
            if (mongo.findById(entree.id(), QuizQuestion.class) != null) {
                continue;
            }
            QuizQuestion q = new QuizQuestion();
            q.setId(entree.id());
            q.setTheme(QuizQuestion.Theme.valueOf(entree.theme().toUpperCase(Locale.ROOT)));
            q.setDate(entree.date());
            q.setSource(entree.source());
            q.setImage(entree.image());
            q.setImagesChoix(entree.imagesChoix());
            q.setFr(new QuizQuestion.Wording(entree.fr().enonce(), entree.fr().choix(), entree.fr().explication()));
            q.setEn(new QuizQuestion.Wording(entree.en().enonce(), entree.en().choix(), entree.en().explication()));
            q.setUpdatedBy("schub");
            q.setUpdatedAt(maintenant);
            mongo.save(q);
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.remove(Query.query(Criteria.where("_id").in(lis().stream().map(Entree::id).toList())
                .and("updatedBy").is("schub")), QuizQuestion.class);
    }

    static List<Entree> lis() {
        try (InputStream flux = new ClassPathResource("quiz/fabriquees.json").getInputStream()) {
            return new ObjectMapper().readValue(flux, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
