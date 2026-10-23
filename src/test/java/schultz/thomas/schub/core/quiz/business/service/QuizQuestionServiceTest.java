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
        QuizQuestionDraft doublon = brouillon(List.of("A", "A", "B", "C"), null);
        QuizQuestionDraft troisChoix = brouillon(List.of("A", "B", "C"), null);
        QuizQuestionDraft valable = brouillon(List.of("A", "B", "C", "D"), null);

        assertThatThrownBy(() -> service.save(owner, "worlds-2011", doublon))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(owner, "worlds-2011", troisChoix))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(owner, "Worlds 2011", valable))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(owner, "a".repeat(40_000) + "-", valable))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Une image et une rangée d'icônes par réponse, en https, ou rien")
    void images() {
        QuizQuestion.Wording texte = new QuizQuestion.Wording("Avec quoi ?", List.of("A", "B", "C", "D"), "Ainsi.");
        List<String> rangee = List.of("https://ddragon.leagueoflegends.com/cdn/16.21.1/img/item/1038.png");
        QuizQuestion q = service.save(owner, "objet-recette-3031", new QuizQuestionDraft(QuizQuestion.Theme.OBJETS,
                "2026-10-23", "https://example.org", " https://example.org/3031.png ",
                List.of(rangee, rangee, rangee, rangee), texte, texte, null));

        assertThat(q.getImage()).isEqualTo("https://example.org/3031.png");
        assertThat(q.getImagesChoix()).hasSize(4);
        QuizQuestionDraft enHttp = new QuizQuestionDraft(QuizQuestion.Theme.OBJETS, "2026", "https://example.org",
                "http://example.org/a.png", null, texte, texte, null);
        QuizQuestionDraft uneRangee = new QuizQuestionDraft(QuizQuestion.Theme.OBJETS, "2026", "https://example.org",
                null, List.of(rangee), texte, texte, null);

        assertThatThrownBy(() -> service.save(owner, "x", enHttp)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(owner, "x", uneRangee)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Sans la permission, ni lecture complète, ni écriture, ni suppression")
    void refuse() {
        doThrow(new AccessDeniedException("non")).when(permissions)
                .require(any(), eq(Permission.QUIZ_QUESTION_EDIT), any());

        QuizQuestionDraft valable = brouillon(List.of("A", "B", "C", "D"), null);

        assertThatThrownBy(() -> service.all(owner)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.save(owner, "x", valable)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.delete(owner, "x")).isInstanceOf(AccessDeniedException.class);
    }

    private static QuizQuestionDraft brouillon(List<String> choix, Boolean active) {
        QuizQuestion.Wording texte = new QuizQuestion.Wording("Qui ?", choix, "Parce que.");
        return new QuizQuestionDraft(QuizQuestion.Theme.PROS, "2011", "https://example.org", null, null, texte, texte,
                active);
    }
}
