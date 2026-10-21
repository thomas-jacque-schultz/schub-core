package schultz.thomas.schub.core.data.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.SystemRole;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

// La banque du quiz, écrite à la main dans le front (Schub#136), versée en base pour le panneau d'admin (Schub#137).
@ChangeUnit(id = "quiz-bank", order = "028", author = "schub")
public class V028_QuizBank {

    record Libelles(String enonce, List<String> choix, String explication) {
    }

    record Entree(String id, String theme, String date, String source, Libelles fr, Libelles en) {
    }

    @Execution
    public void execution(MongoTemplate mongo) {
        Instant maintenant = Instant.now();
        for (Entree entree : lis()) {
            QuizQuestion q = new QuizQuestion();
            q.setId(entree.id());
            q.setTheme(QuizQuestion.Theme.valueOf(entree.theme().toUpperCase(Locale.ROOT)));
            q.setDate(entree.date());
            q.setSource(entree.source());
            q.setFr(new QuizQuestion.Wording(entree.fr().enonce(), entree.fr().choix(), entree.fr().explication()));
            q.setEn(new QuizQuestion.Wording(entree.en().enonce(), entree.en().choix(), entree.en().explication()));
            q.setUpdatedBy("schub");
            q.setUpdatedAt(maintenant);
            mongo.save(q);
        }
        mongo.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.OWNER.roleName()),
                Updates.addToSet("permissions", Permission.QUIZ_QUESTION_EDIT.name()));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.dropCollection(QuizQuestion.class);
        mongo.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.OWNER.roleName()),
                Updates.pull("permissions", Permission.QUIZ_QUESTION_EDIT.name()));
    }

    static List<Entree> lis() {
        try (InputStream flux = new ClassPathResource("quiz/banque.json").getInputStream()) {
            return new ObjectMapper().readValue(flux, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
