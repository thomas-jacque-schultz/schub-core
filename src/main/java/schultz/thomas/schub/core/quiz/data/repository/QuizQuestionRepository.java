package schultz.thomas.schub.core.quiz.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.util.List;

public interface QuizQuestionRepository extends MongoRepository<QuizQuestion, String> {

    List<QuizQuestion> findByActiveTrueOrderById();

    List<QuizQuestion> findAllByOrderById();
}
