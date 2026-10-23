package schultz.thomas.schub.core.quiz.api.dto;

import jakarta.annotation.Nullable;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.time.Instant;
import java.util.List;

public record QuizQuestionDto(String id, QuizQuestion.Theme theme, String date, String source, @Nullable String image,
                              @Nullable List<List<String>> imagesChoix, QuizWordingDto fr, QuizWordingDto en,
                              boolean active, Instant updatedAt) {
}
