package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.api.dto.MeDto;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.RiotAccountState;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le nom d'affichage, et ce que {@code GET /me} rassemble.
 *
 * <p>Le piège de ce champ est le repli : tant qu'il n'est pas choisi, c'est le pseudo Discord qui
 * s'affiche, et il doit continuer de suivre ses changements. Le recopier une fois donnerait un
 * nom qui vieillit sans que personne ne s'en aperçoive.</p>
 */
class UserServiceDisplayNameTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PermissionEvaluator permissionEvaluator;
    private RiotAccountService riotAccountService;
    private UserService userService;

    private User acteur;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        permissionEvaluator = mock(PermissionEvaluator.class);
        riotAccountService = mock(RiotAccountService.class);
        userService = new UserService(userRepository, roleRepository, permissionEvaluator, riotAccountService);

        acteur = new User();
        acteur.setId("user-1");
        acteur.setDiscordId("discord-1");
        acteur.setDiscordUsername("thomas");
        acteur.setAvatarUrl("https://cdn.discordapp.com/avatars/1/a.png");
        acteur.setRoleId("role-visiteur");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Sans nom choisi, c'est le pseudo Discord qui s'affiche — et il reste à jour")
    void replieSurLePseudoDiscord() {
        assertThat(userService.displayNameOf(acteur)).isEqualTo("thomas");

        acteur.setDiscordUsername("thomas-a-change");

        assertThat(userService.displayNameOf(acteur))
                .as("rien n'a été recopié, donc le pseudo Discord suit")
                .isEqualTo("thomas-a-change");
    }

    @Test
    @DisplayName("Un nom choisi remplace le pseudo Discord")
    void prendLeNomChoisi() {
        userService.changeDisplayName(acteur, "  Le Capitaine  ");

        assertThat(acteur.getDisplayName()).isEqualTo("Le Capitaine");
        assertThat(userService.displayNameOf(acteur)).isEqualTo("Le Capitaine");
    }

    @Test
    @DisplayName("Un nom vide rend le pseudo Discord : c'est le seul moyen d'annuler")
    void rendLeNomParDefaut() {
        acteur.setDisplayName("Le Capitaine");

        userService.changeDisplayName(acteur, "   ");

        assertThat(acteur.getDisplayName()).isNull();
        assertThat(userService.displayNameOf(acteur)).isEqualTo("thomas");
    }

    @Test
    @DisplayName("Deux comptes peuvent porter le même nom : ce n'est pas un identifiant")
    void nExigePasLUnicite() {
        userService.changeDisplayName(acteur, "Thomas");

        assertThat(acteur.getDisplayName()).isEqualTo("Thomas");
        verify(userRepository, never()).findByDiscordUsername(any());
    }

    @Test
    @DisplayName("Ce qui casserait un écran est refusé : trop long, ou rien d'affichable")
    void refuseCeQuiCasseraitUnEcran() {
        assertThatThrownBy(() -> userService.changeDisplayName(acteur, "n".repeat(33)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.changeDisplayName(acteur, "!!! ---"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GET /me rassemble identité, rôle, permissions et compte Riot en un appel")
    void rassembleLeProfilEnUnAppel() {
        Role role = new Role();
        role.setId("role-visiteur");
        role.setName(SystemRole.VISITEUR.roleName());
        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(role));
        when(permissionEvaluator.rolePermissions(acteur)).thenReturn(Set.of(Permission.SERVER_VIEW));
        when(riotAccountService.of(acteur)).thenReturn(
                new RiotAccountDto(RiotAccountState.ABSENT, null, null, null, null, null, null));

        MeDto me = userService.toMeDto(acteur);

        assertThat(me.userId()).isEqualTo("user-1");
        assertThat(me.discord().id()).isEqualTo("discord-1");
        assertThat(me.discord().avatarUrl()).isNotNull();
        assertThat(me.displayName()).isEqualTo("thomas");
        assertThat(me.role().name()).isEqualTo(SystemRole.VISITEUR.roleName());
        assertThat(me.role().permissions()).containsExactly(Permission.SERVER_VIEW);
        assertThat(me.riot().state()).isEqualTo(RiotAccountState.ABSENT);
    }
}
