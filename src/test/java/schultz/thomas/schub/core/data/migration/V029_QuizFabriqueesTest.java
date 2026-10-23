package schultz.thomas.schub.core.data.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class V029_QuizFabriqueesTest {

    @Test
    @DisplayName("Le lot se lit en entier, chaque thème existe, la bonne réponse a ses icônes quand il y en a")
    void lot() {
        var lot = V029_QuizFabriquees.lis();

        assertThat(lot).hasSizeGreaterThan(100);
        assertThat(lot).allSatisfy(entree -> {
            assertThat(QuizQuestion.Theme.valueOf(entree.theme().toUpperCase(Locale.ROOT))).isNotNull();
            assertThat(entree.fr().choix()).hasSize(4);
            assertThat(entree.en().choix()).hasSize(4);
            if (entree.imagesChoix() != null) {
                assertThat(entree.imagesChoix()).hasSize(4);
            }
        });
    }
}
