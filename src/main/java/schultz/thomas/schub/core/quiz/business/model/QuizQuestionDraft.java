package schultz.thomas.schub.core.quiz.business.model;

import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

public record QuizQuestionDraft(QuizQuestion.Theme theme, String date, String source, QuizQuestion.Wording fr,
                                QuizQuestion.Wording en, Boolean active) {
}
