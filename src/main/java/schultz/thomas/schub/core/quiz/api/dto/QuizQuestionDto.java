package schultz.thomas.schub.core.quiz.api.dto;

import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.time.Instant;

public record QuizQuestionDto(String id, QuizQuestion.Theme theme, String date, String source, QuizWordingDto fr,
                              QuizWordingDto en, boolean active, Instant updatedAt) {
}
