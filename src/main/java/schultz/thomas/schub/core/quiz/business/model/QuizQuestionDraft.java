package schultz.thomas.schub.core.quiz.business.model;

import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.util.List;

public record QuizQuestionDraft(QuizQuestion.Theme theme, String date, String source, String image,
                                List<List<String>> imagesChoix, QuizQuestion.Wording fr,
                                QuizQuestion.Wording en, Boolean active) {
}
