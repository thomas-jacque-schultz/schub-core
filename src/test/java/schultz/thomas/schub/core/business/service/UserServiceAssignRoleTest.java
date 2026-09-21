package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.EnumSet;
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
 * La règle anti-élévation, et les trois garde-fous qui l'accompagnent.
 *
 * <p>C'est le chemin d'élévation classique d'un modèle rôles/permissions : se fabriquer un rôle
 * « tout coché » et se l'attribuer. Il se ferme en trois lignes — encore faut-il qu'elles soient
 * là, et qu'elles y restent. D'où ces tests.</p>
 */
class UserServiceAssignRoleTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PermissionEvaluator permissionEvaluator;
    private UserService userService;

    private User acteur;
    private User cible;
    private Role ownerRole;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        permissionEvaluator = mock(PermissionEvaluator.class);
        userService = new UserService(userRepository, roleRepository, permissionEvaluator,
                mock(RiotAccountService.class));

        acteur = utilisateur("acteur", "role-admin");
        cible = utilisateur("cible", "role-visiteur");

        ownerRole = role("role-owner", SystemRole.OWNER.roleName(), SystemRole.OWNER.permissions());
        when(roleRepository.findByName(SystemRole.OWNER.roleName())).thenReturn(Optional.of(ownerRole));
        when(userRepository.findById("cible")).thenReturn(Optional.of(cible));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // L'acteur est ADMINISTRATOR : tout sauf ROLE_MANAGE.
        when(permissionEvaluator.rolePermissions(acteur))
                .thenReturn(EnumSet.copyOf(SystemRole.ADMINISTRATOR.permissions()));
    }

    @Test
    @DisplayName("attribue un rôle dont les permissions sont un sous-ensemble des siennes")
    void attributionAutorisee() {
        Role moderateur = role("role-modo", "MODERATOR", SystemRole.MODERATOR.permissions());
        when(roleRepository.findById("role-modo")).thenReturn(Optional.of(moderateur));

        User result = userService.assignRole(acteur, "cible", "role-modo");

        assertThat(result.getRoleId()).isEqualTo("role-modo");
    }

    @Test
    @DisplayName("refuse un rôle plus puissant que le sien — le chemin d'élévation classique")
    void refuseUnRolePlusPuissant() {
        // L'acteur est rabaissé à MODERATOR pour isoler la règle du sous-ensemble : avec un
        // acteur ADMINISTRATOR, tout rôle sans ROLE_MANAGE lui est déjà inférieur, et c'est
        // l'interdit « ROLE_MANAGE jamais attribuable » qui refuserait — pas ce qu'on teste ici.
        when(permissionEvaluator.rolePermissions(acteur))
                .thenReturn(EnumSet.copyOf(SystemRole.MODERATOR.permissions()));
        Role administrateur = role("role-admin-cible", "ADMINISTRATOR", SystemRole.ADMINISTRATOR.permissions());
        when(roleRepository.findById("role-admin-cible")).thenReturn(Optional.of(administrateur));

        assertThatThrownBy(() -> userService.assignRole(acteur, "cible", "role-admin-cible"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("plus puissant que le sien");
        verify(userRepository, never()).save(cible);
    }

    @Test
    @DisplayName("ROLE_MANAGE n'est jamais attribuable, même par qui la détient")
    void refuseRoleManage() {
        Role gestionnaire = role("role-gestion", "GESTIONNAIRE", EnumSet.of(Permission.ROLE_MANAGE));
        when(roleRepository.findById("role-gestion")).thenReturn(Optional.of(gestionnaire));
        when(permissionEvaluator.rolePermissions(acteur)).thenReturn(EnumSet.allOf(Permission.class));

        assertThatThrownBy(() -> userService.assignRole(acteur, "cible", "role-gestion"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("n'est pas attribuable");
    }

    @Test
    @DisplayName("le rôle OWNER n'est pas attribuable depuis l'API")
    void refuseOwner() {
        when(roleRepository.findById("role-owner")).thenReturn(Optional.of(ownerRole));
        when(permissionEvaluator.rolePermissions(acteur)).thenReturn(EnumSet.allOf(Permission.class));

        assertThatThrownBy(() -> userService.assignRole(acteur, "cible", "role-owner"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("n'est pas attribuable");
    }

    @Test
    @DisplayName("on ne modifie pas son propre rôle")
    void refuseSonPropreRole() {
        when(userRepository.findById("acteur")).thenReturn(Optional.of(acteur));
        Role moderateur = role("role-modo", "MODERATOR", SystemRole.MODERATOR.permissions());
        when(roleRepository.findById("role-modo")).thenReturn(Optional.of(moderateur));

        assertThatThrownBy(() -> userService.assignRole(acteur, "acteur", "role-modo"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("son propre rôle");
    }

    @Test
    @DisplayName("le dernier OWNER ne peut pas être rétrogradé — sinon plus personne n'a ROLE_MANAGE")
    void refuseDeRetrograderLeDernierOwner() {
        cible.setRoleId("role-owner");
        when(userRepository.countByRoleId("role-owner")).thenReturn(1L);
        Role moderateur = role("role-modo", "MODERATOR", SystemRole.MODERATOR.permissions());
        when(roleRepository.findById("role-modo")).thenReturn(Optional.of(moderateur));

        assertThatThrownBy(() -> userService.assignRole(acteur, "cible", "role-modo"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("dernier");
    }

    @Test
    @DisplayName("la règle du sous-ensemble couvre les permissions d'équipe sans avoir été rouverte")
    void refuseUneElevationParLesPermissionsDEquipe() {
        // Un compte au rôle VISITEUR : il peut créer une équipe, rien de plus. Qu'il puisse
        // distribuer TEAM_EDIT — le droit d'écrire dans l'équipe des autres — serait exactement
        // l'élévation que la règle ferme. Le test est ici parce que la règle est générique : si
        // quelqu'un la remplace un jour par une liste blanche, c'est ce test qui tombera.
        when(permissionEvaluator.rolePermissions(acteur))
                .thenReturn(EnumSet.copyOf(SystemRole.VISITEUR.permissions()));
        Role capitaine = role("role-capitaine", "CAPITAINE",
                EnumSet.of(Permission.TEAM_CREATE, Permission.TEAM_VIEW, Permission.TEAM_EDIT));
        when(roleRepository.findById("role-capitaine")).thenReturn(Optional.of(capitaine));

        assertThatThrownBy(() -> userService.assignRole(acteur, "cible", "role-capitaine"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("plus puissant que le sien");
        verify(userRepository, never()).save(cible);
    }

    @Test
    @DisplayName("un rôle limité à TEAM_CREATE reste attribuable par qui la détient")
    void attribueUnRoleDEquipeQuOnDetientDeja() {
        when(permissionEvaluator.rolePermissions(acteur))
                .thenReturn(EnumSet.copyOf(SystemRole.MODERATOR.permissions()));
        Role joueur = role("role-joueur", "JOUEUR",
                EnumSet.of(Permission.SERVER_VIEW, Permission.TEAM_CREATE));
        when(roleRepository.findById("role-joueur")).thenReturn(Optional.of(joueur));

        assertThat(userService.assignRole(acteur, "cible", "role-joueur").getRoleId())
                .isEqualTo("role-joueur");
    }

    private User utilisateur(String id, String roleId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId("discord-" + id);
        user.setRoleId(roleId);
        return user;
    }

    private Role role(String id, String name, Set<Permission> permissions) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setPermissions(EnumSet.copyOf(permissions));
        return role;
    }
}
