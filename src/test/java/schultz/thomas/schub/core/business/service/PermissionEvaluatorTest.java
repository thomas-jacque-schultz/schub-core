package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les deux sources d'autorité, et surtout leur frontière.
 *
 * <p>Ce qui est vérifié ici n'est pas que l'évaluateur dit oui quand il faut — c'est qu'il dit
 * <strong>non</strong> aux trois cas qui rendraient le modèle décoratif : un admin de serveur qui
 * déborderait sur un autre serveur, un admin de serveur qui obtiendrait autre chose que
 * démarrer/arrêter, et une question globale qui répondrait oui parce que l'acteur est admin
 * quelque part.</p>
 */
class PermissionEvaluatorTest {

    private static final String ACTEUR_ID = "user-1";

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private GameServerService gameServerService;
    private PermissionEvaluator evaluator;

    private User acteur;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        gameServerService = mock(GameServerService.class);
        evaluator = new PermissionEvaluator(userRepository, roleRepository, gameServerService, List.of());

        acteur = new User();
        acteur.setId(ACTEUR_ID);
        acteur.setDiscordId("42");
        acteur.setRoleId("role-visiteur");

        Role visiteur = new Role();
        visiteur.setId("role-visiteur");
        visiteur.setName("VISITEUR");
        visiteur.setPermissions(EnumSet.of(Permission.SERVER_VIEW));
        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(visiteur));
        when(gameServerService.findBySlug(anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("les permissions du rôle s'appliquent partout")
    void permissionsDuRole() {
        assertThat(evaluator.can(acteur, Permission.SERVER_VIEW, null)).isTrue();
        assertThat(evaluator.can(acteur, Permission.SERVER_START, null)).isFalse();
    }

    @Test
    @DisplayName("être admin d'un serveur donne START et STOP sur CE serveur")
    void adminDeServeur() {
        donneLeServeur("minecraft", ACTEUR_ID);

        ResourceRef ressource = ResourceRef.gameServer("minecraft");
        assertThat(evaluator.can(acteur, Permission.SERVER_START, ressource)).isTrue();
        assertThat(evaluator.can(acteur, Permission.SERVER_STOP, ressource)).isTrue();
    }

    @Test
    @DisplayName("être admin d'un serveur ne donne rien de plus que START et STOP")
    void adminDeServeurNeDebordePas() {
        donneLeServeur("minecraft", ACTEUR_ID);

        ResourceRef ressource = ResourceRef.gameServer("minecraft");
        assertThat(evaluator.can(acteur, Permission.SERVER_EDIT, ressource)).isFalse();
        assertThat(evaluator.can(acteur, Permission.PORT_RULE_EDIT, ressource)).isFalse();
        assertThat(evaluator.can(acteur, Permission.SERVER_INFRA_VIEW, ressource)).isFalse();
    }

    @Test
    @DisplayName("être admin d'un serveur ne donne aucun droit sur un autre")
    void adminDeServeurNeDeborderPasSurUnAutre() {
        donneLeServeur("minecraft", ACTEUR_ID);
        donneLeServeur("valheim", "quelquun-dautre");

        assertThat(evaluator.can(acteur, Permission.SERVER_START, ResourceRef.gameServer("valheim"))).isFalse();
    }

    @Test
    @DisplayName("sans ressource, la question est globale : être admin quelque part ne compte pas")
    void questionGlobale() {
        donneLeServeur("minecraft", ACTEUR_ID);

        assertThat(evaluator.can(acteur, Permission.SERVER_START, null)).isFalse();
    }

    @Test
    @DisplayName("un rôle introuvable ne donne aucun droit, il n'en donne pas tous")
    void roleIntrouvable() {
        acteur.setRoleId("role-fantome");
        when(roleRepository.findById("role-fantome")).thenReturn(Optional.empty());

        assertThat(evaluator.rolePermissions(acteur)).isEmpty();
        assertThat(evaluator.can(acteur, Permission.SERVER_VIEW, null)).isFalse();
    }

    @Test
    @DisplayName("require refuse en AccessDeniedException plutôt qu'en booléen ignoré")
    void requireRefuse() {
        assertThatThrownBy(() -> evaluator.require(acteur, Permission.SERVER_DELETE, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SERVER_DELETE");
    }

    private void donneLeServeur(String slug, String adminId) {
        GameServer server = new GameServer();
        server.setSlug(slug);
        server.setAdmins(List.of(adminId));
        when(gameServerService.findBySlug(slug)).thenReturn(Optional.of(server));
    }
}
