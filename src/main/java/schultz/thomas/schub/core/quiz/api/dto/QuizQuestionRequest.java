package schultz.thomas.schub.core.quiz.api.dto;

import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

public record QuizQuestionRequest(QuizQuestion.Theme theme, String date, String source, QuizWordingDto fr,
                                  QuizWordingDto en, Boolean active) {
}
