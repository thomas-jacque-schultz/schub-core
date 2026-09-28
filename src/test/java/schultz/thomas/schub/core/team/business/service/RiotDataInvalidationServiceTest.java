package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import schultz.thomas.schub.core.augur.data.repository.FindingRecordRepository;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.api.dto.RiotDataInvalidationDto;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RiotDataInvalidationServiceTest {

    private final RiotDataGateway riotData = mock(RiotDataGateway.class);
    private final StalePuuidRepair repair = mock(StalePuuidRepair.class);
    private final FindingRecordRepository findings = mock(FindingRecordRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final GameReviewRepository reviews = mock(GameReviewRepository.class);
    private final RiotDataInvalidationService service =
            new RiotDataInvalidationService(riotData, repair, findings, users, teams, reviews);

    @Test
    @DisplayName("Sans la saisie exacte de INVALIDER, rien n'est effacé")
    void confirmationExigee() {
        assertThatThrownBy(() -> service.invalidate(owner(), "invalider"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(riotData);
        verify(findings, never()).deleteAll();
        verify(repair, never()).resoutTout(any());
    }

    @Test
    @DisplayName("Les puuid suivis sont relevés avant l'effacement, puis résolus de nouveau par leur Riot ID")
    void effacePuisResout() {
        Set<String> suivis = new LinkedHashSet<>(List.of("pisel", "buluc"));
        when(repair.puuidsSuivis()).thenReturn(suivis);
        when(riotData.purge()).thenReturn(Map.of("riot_match", 74_000L, "riot_known_account", 478_000L));
        when(findings.count()).thenReturn(1_200L);

        RiotDataInvalidationDto rapport = service.invalidate(owner(), RiotDataInvalidationService.CONFIRMATION);

        var ordre = inOrder(repair, riotData, findings);
        ordre.verify(repair).puuidsSuivis();
        ordre.verify(riotData).purge();
        ordre.verify(findings).deleteAll();
        ordre.verify(repair).resoutTout(suivis);
        assertThat(rapport).isEqualTo(new RiotDataInvalidationDto(552_000L, 1_200L, 2));
    }

    private static User owner() {
        User user = new User();
        user.setId("owner");
        return user;
    }
}
