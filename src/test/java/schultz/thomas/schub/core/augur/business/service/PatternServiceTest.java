package schultz.thomas.schub.core.augur.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.augur.api.dto.PatternDto;
import schultz.thomas.schub.core.augur.api.dto.PatternRequest;
import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;
import schultz.thomas.schub.core.augur.data.repository.FindingRecordRepository;
import schultz.thomas.schub.core.augur.data.repository.PatternVersionRepository;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatternServiceTest {

    private final PatternVersionRepository patterns = mock(PatternVersionRepository.class);
    private final PatternService service = new PatternService(patterns, mock(FindingRecordRepository.class),
            mock(PermissionEvaluator.class), mock(Sensors.class), mock(PatternService.PatternRefresher.class));
    private final User owner = new User();

    @BeforeEach
    void setUp() {
        owner.setId("owner");
        when(patterns.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    @DisplayName("Une nouvelle règle naît expérimentale")
    void nouvelleRegleExperimentale() {
        when(patterns.findByKeyOrderByVersionDesc("regle-neuve")).thenReturn(List.of());

        assertThat(service.draft(owner, requete("regle-neuve", null)).experimental()).isTrue();
    }

    @Test
    @DisplayName("Une nouvelle version garde le drapeau de la précédente, sauf choix explicite dans l'éditeur")
    void nouvelleVersionHerite() {
        PatternVersion validee = new PatternVersion();
        validee.setKey("regle-validee");
        validee.setVersion(2);
        validee.setStatus(PatternVersion.Status.ACTIVE);
        when(patterns.findByKeyOrderByVersionDesc("regle-validee")).thenReturn(List.of(validee));

        assertThat(service.draft(owner, requete("regle-validee", null)).experimental()).isFalse();
        PatternDto cochee = service.draft(owner, requete("regle-validee", true));
        assertThat(cochee.experimental()).isTrue();
        assertThat(cochee.limits()).containsEntry("fr", "Mesure à la minute.");
    }

    private static PatternRequest requete(String key, Boolean experimental) {
        return new PatternRequest(key, PatternVersion.Scope.GAME, PatternVersion.Polarity.WEAKNESS,
                PatternVersion.Category.BEHAVIOUR, PatternVersion.Nature.ACTION,
                List.of(new Condition("deaths", Condition.Unit.VALUE, 5, 10, 1)), null, null, null, null,
                Map.of("fr", "Titre"), Map.of("fr", "Phrase"), experimental, Map.of("fr", "Mesure à la minute."),
                "test");
    }
}
