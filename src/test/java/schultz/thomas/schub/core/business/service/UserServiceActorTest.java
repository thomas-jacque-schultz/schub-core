package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.api.dto.AccountLinksDto;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceActorTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository, mock(RoleRepository.class),
                mock(PermissionEvaluator.class), mock(RiotAccountService.class));
    }

    @Test
    @DisplayName("X-Actor-Id désigne l'identifiant interne, plus l'identifiant Discord")
    void resoutLActeurParSonIdentifiantInterne() {
        User compte = new User();
        compte.setId("user-1");
        compte.setDiscordId("123456789");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(compte));

        assertThat(userService.requireActor("user-1")).isSameAs(compte);
        assertThatThrownBy(() -> userService.requireActor("123456789"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Un compte sans Discord est un acteur comme un autre, et ses liens le disent")
    void compteSansDiscord() {
        User riotSeul = new User();
        riotSeul.setId("user-2");
        riotSeul.setRiotPuuid("puuid-2");
        when(userRepository.findById("user-2")).thenReturn(Optional.of(riotSeul));

        assertThat(userService.requireActor("user-2")).isSameAs(riotSeul);
        assertThat(AccountLinksDto.of(riotSeul)).isEqualTo(new AccountLinksDto(false, true));
    }

    @Test
    @DisplayName("En-tête absent : refus")
    void enteteAbsent() {
        assertThatThrownBy(() -> userService.requireActor(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userService.requireActor(" ")).isInstanceOf(AccessDeniedException.class);
    }
}
