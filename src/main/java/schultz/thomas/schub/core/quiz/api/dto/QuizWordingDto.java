package schultz.thomas.schub.core.quiz.api.dto;

import java.util.List;

// La bonne réponse est la première de choix.
public record QuizWordingDto(String enonce, List<String> choix, String explication) {
}
