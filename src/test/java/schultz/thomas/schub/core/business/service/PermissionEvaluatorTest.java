package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les deux sources d'autorité, et surtout leur frontière.
 *
 * <p>Ce qui est vérifié ici n'est pas que l'évaluateur dit oui quand il faut — c'est qu'il dit
 * <strong>non</strong> aux cas qui rendraient le modèle décoratif : une appartenance qui
 * déborderait sur une autre ressource, et une question globale qui répondrait oui parce que
 * l'acteur appartient à quelque chose quelque part.</p>
 */
class PermissionEvaluatorTest {

    private static final String ACTEUR_ID = "user-1";

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PermissionEvaluator evaluator;

    private User acteur;

    /** Un domaine qui accorde {@code TEAM_EDIT} sur une seule ressource, et rien ailleurs. */
    private static ScopedAuthorityProvider surLaRessource(String resourceId, Permission accordee) {
        return new ScopedAuthorityProvider() {
            @Override
            public ResourceType resourceType() {
                return ResourceType.TEAM;
            }

            @Override
            public Set<Permission> grantedTo(User actor, String id) {
                return resourceId.equals(id) ? Set.of(accordee) : Set.of();
            }
        };
    }

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        evaluator = new PermissionEvaluator(userRepository, roleRepository,
                List.of(surLaRessource("equipe-1", Permission.TEAM_EDIT)));

        acteur = new User();
        acteur.setId(ACTEUR_ID);
        acteur.setDiscordId("42");
        acteur.setRoleId("role-visiteur");

        Role visiteur = new Role();
        visiteur.setId("role-visiteur");
        visiteur.setName("VISITEUR");
        visiteur.setPermissions(EnumSet.of(Permission.SERVER_VIEW));
        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(visiteur));
    }

    @Test
    @DisplayName("les permissions du rôle s'appliquent partout")
    void permissionsDuRole() {
        assertThat(evaluator.can(acteur, Permission.SERVER_VIEW, null)).isTrue();
        assertThat(evaluator.can(acteur, Permission.SERVER_START, null)).isFalse();
    }

    /**
     * La régression que le point 3 corrige : démarrer un serveur ne dépend plus que du rôle,
     * donc un modérateur peut le faire sur tous les serveurs et sans qu'on l'y ait inscrit.
     */
    @Test
    @DisplayName("piloter un serveur vient du rôle seul, sans ressource")
    void piloterVientDuRole() {
        Role moderateur = new Role();
        moderateur.setId("role-modo");
        moderateur.setName("MODERATOR");
        moderateur.setPermissions(EnumSet.of(Permission.SERVER_VIEW, Permission.SERVER_START,
                Permission.SERVER_STOP));
        when(roleRepository.findById("role-modo")).thenReturn(Optional.of(moderateur));
        acteur.setRoleId("role-modo");

        assertThat(evaluator.can(acteur, Permission.SERVER_START, null)).isTrue();
        assertThat(evaluator.can(acteur, Permission.SERVER_STOP, null)).isTrue();
        assertThat(evaluator.can(acteur, Permission.SERVER_EDIT, null)).isFalse();
    }

    @Test
    @DisplayName("appartenir à une ressource donne un droit sur CETTE ressource")
    void porteeDeRessource() {
        assertThat(evaluator.can(acteur, Permission.TEAM_EDIT, ResourceRef.team("equipe-1"))).isTrue();
    }

    @Test
    @DisplayName("appartenir à une ressource ne donne rien sur une autre")
    void porteeQuiNeDebordePas() {
        assertThat(evaluator.can(acteur, Permission.TEAM_EDIT, ResourceRef.team("equipe-2"))).isFalse();
    }

    @Test
    @DisplayName("sans ressource, la question est globale : appartenir quelque part ne compte pas")
    void questionGlobale() {
        assertThat(evaluator.can(acteur, Permission.TEAM_EDIT, null)).isFalse();
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
}
