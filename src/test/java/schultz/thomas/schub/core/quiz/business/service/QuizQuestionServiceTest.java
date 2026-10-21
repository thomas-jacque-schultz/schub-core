package schultz.thomas.schub.core.quiz.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.quiz.business.model.QuizQuestionDraft;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;
import schultz.thomas.schub.core.quiz.data.repository.QuizQuestionRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuizQuestionServiceTest {

    private final QuizQuestionRepository questions = mock(QuizQuestionRepository.class);
    private final PermissionEvaluator permissions = mock(PermissionEvaluator.class);
    private final QuizQuestionService service = new QuizQuestionService(questions, permissions);
    private final User owner = new User();

    @BeforeEach
    void setUp() {
        owner.setId("owner");
        when(questions.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(questions.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Une question enregistrée garde son auteur, et se désactive sans disparaître")
    void enregistre() {
        QuizQuestion q = service.save(owner, "worlds-2011", brouillon(List.of("Fnatic", "A", "B", "C"), false));

        assertThat(q.getId()).isEqualTo("worlds-2011");
        assertThat(q.getUpdatedBy()).isEqualTo("owner");
        assertThat(q.isActive()).isFalse();
    }

    @Test
    @DisplayName("Quatre réponses différentes, une source en https, un identifiant propre")
    void valide() {
        assertThatThrownBy(() -> service.save(owner, "worlds-2011", brouillon(List.of("A", "A", "B", "C"), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(owner, "worlds-2011", brouillon(List.of("A", "B", "C"), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(owner, "Worlds 2011", brouillon(List.of("A", "B", "C", "D"), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Sans la permission, ni lecture complète ni écriture")
    void refuse() {
        doThrow(new AccessDeniedException("non")).when(permissions)
                .require(any(), eq(Permission.QUIZ_QUESTION_EDIT), any());

        assertThatThrownBy(() -> service.all(owner)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.save(owner, "x", brouillon(List.of("A", "B", "C", "D"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static QuizQuestionDraft brouillon(List<String> choix, Boolean active) {
        QuizQuestion.Wording texte = new QuizQuestion.Wording("Qui ?", choix, "Parce que.");
        return new QuizQuestionDraft(QuizQuestion.Theme.PROS, "2011", "https://example.org", texte, texte, active);
    }
}
