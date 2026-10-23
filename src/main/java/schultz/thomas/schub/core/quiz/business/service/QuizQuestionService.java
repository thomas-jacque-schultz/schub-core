package schultz.thomas.schub.core.quiz.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.quiz.business.model.QuizQuestionDraft;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;
import schultz.thomas.schub.core.quiz.data.repository.QuizQuestionRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuizQuestionService {

    private static final Pattern ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final int CHOIX = 4;
    private static final int TEXTE_MAX = 600;
    private static final int ICONES_MAX = 6;

    private final QuizQuestionRepository questions;
    private final PermissionEvaluator permissionEvaluator;

    public List<QuizQuestion> active() {
        return questions.findByActiveTrueOrderById();
    }

    public List<QuizQuestion> all(User actor) {
        permissionEvaluator.require(actor, Permission.QUIZ_QUESTION_EDIT, null);
        return questions.findAllByOrderById();
    }

    // Créer ou remplacer : l'identifiant vient de l'adresse.
    public QuizQuestion save(User actor, String id, QuizQuestionDraft draft) {
        permissionEvaluator.require(actor, Permission.QUIZ_QUESTION_EDIT, null);
        valide(id, draft);
        QuizQuestion question = questions.findById(id).orElseGet(QuizQuestion::new);
        question.setId(id);
        question.setTheme(draft.theme());
        question.setDate(draft.date().trim());
        question.setSource(draft.source().trim());
        question.setImage(vide(draft.image()) ? null : draft.image().trim());
        question.setImagesChoix(draft.imagesChoix());
        question.setFr(propre(draft.fr()));
        question.setEn(propre(draft.en()));
        if (draft.active() != null) {
            question.setActive(draft.active());
        }
        question.setUpdatedBy(actor.getId());
        question.setUpdatedAt(Instant.now());
        return questions.save(question);
    }

    public void delete(User actor, String id) {
        permissionEvaluator.require(actor, Permission.QUIZ_QUESTION_EDIT, null);
        questions.deleteById(id);
    }

    static void valide(String id, QuizQuestionDraft draft) {
        if (id == null || !ID.matcher(id).matches() || id.length() > 80) {
            throw new IllegalArgumentException("Identifiant invalide : minuscules, chiffres et tirets.");
        }
        if (draft == null || draft.theme() == null || vide(draft.date())) {
            throw new IllegalArgumentException("Thème et date obligatoires.");
        }
        if (!https(draft.source())) {
            throw new IllegalArgumentException("La source est un lien https.");
        }
        if (!vide(draft.image()) && !https(draft.image())) {
            throw new IllegalArgumentException("L'image est un lien https.");
        }
        List<List<String>> rangees = draft.imagesChoix();
        if (rangees != null && (rangees.size() != CHOIX || rangees.stream().anyMatch(rangee -> rangee == null
                || rangee.isEmpty() || rangee.size() > ICONES_MAX
                || rangee.stream().anyMatch(icone -> !https(icone))))) {
            throw new IllegalArgumentException("Une rangée de une à six icônes https par réponse, ou aucune.");
        }
        valide(draft.fr());
        valide(draft.en());
    }

    private static void valide(QuizQuestion.Wording wording) {
        if (wording == null || vide(wording.enonce()) || vide(wording.explication())
                || wording.enonce().length() > TEXTE_MAX || wording.explication().length() > TEXTE_MAX) {
            throw new IllegalArgumentException("Énoncé et explication obligatoires, dans les deux langues.");
        }
        List<String> choix = wording.choix();
        if (choix == null || choix.size() != CHOIX || choix.stream().anyMatch(QuizQuestionService::vide)
                || new HashSet<>(choix.stream().map(String::trim).toList()).size() != CHOIX) {
            throw new IllegalArgumentException("Quatre réponses différentes, la bonne en premier.");
        }
    }

    private static QuizQuestion.Wording propre(QuizQuestion.Wording wording) {
        return new QuizQuestion.Wording(wording.enonce().trim(), wording.choix().stream().map(String::trim).toList(),
                wording.explication().trim());
    }

    private static boolean https(String lien) {
        return lien != null && lien.trim().startsWith("https://");
    }

    private static boolean vide(String texte) {
        return texte == null || texte.isBlank();
    }
}
