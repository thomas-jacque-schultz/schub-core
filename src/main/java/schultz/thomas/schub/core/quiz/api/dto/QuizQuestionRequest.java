package schultz.thomas.schub.core.quiz.api.dto;

import jakarta.annotation.Nullable;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.util.List;

public record QuizQuestionRequest(QuizQuestion.Theme theme, String date, String source, @Nullable String image,
                                  @Nullable List<List<String>> imagesChoix, QuizWordingDto fr, QuizWordingDto en,
                                  Boolean active) {
}
