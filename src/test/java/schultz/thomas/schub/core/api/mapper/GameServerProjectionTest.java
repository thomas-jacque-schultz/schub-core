package schultz.thomas.schub.core.api.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.GameServerMemberDto;
import schultz.thomas.schub.core.business.service.GameServerControlPolicy;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Game;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameServerProjectionTest {

    private PermissionEvaluator permissionEvaluator;
    private GameServerProjection projection;

    private GameServer serveur;

    @BeforeEach
    void setUp() {
        permissionEvaluator = mock(PermissionEvaluator.class);
        UserService userService = mock(UserService.class);
        projection = new GameServerProjection(new GameServerMapperImpl(), permissionEvaluator,
                new GameServerControlPolicy(permissionEvaluator), userService);

        serveur = new GameServer();
        serveur.setId("srv-1");
        serveur.setSlug("minecraft-1");
        serveur.setName("Le serveur");
        serveur.setGame(Game.values()[0]);
        serveur.setDeploymentId(12);
    }

    private User compte(String idInterne) {
        User user = new User();
        user.setId(idInterne);
        user.setDiscordId("227883780512153610");
        return user;
    }

    private void vueInfra(boolean accordee) {
        when(permissionEvaluator.can(any(User.class), eq(Permission.SERVER_INFRA_VIEW), isNull()))
                .thenReturn(accordee);
    }

    @Test
    @DisplayName("sans SERVER_INFRA_VIEW, la projection membre ne décrit pas l'installation")
    void projectionMembre() {
        vueInfra(false);

        Object projete = projection.project(serveur, compte("user-1"));

        assertThat(projete).isInstanceOf(GameServerMemberDto.class);
        assertThat(((GameServerMemberDto) projete).name()).isEqualTo("Le serveur");
    }

    @Test
    @DisplayName("avec SERVER_INFRA_VIEW, le déploiement et les ports arrivent")
    void projectionInfra() {
        vueInfra(true);

        GameServerDto dto = (GameServerDto) projection.project(serveur, compte("user-1"));

        assertThat(dto.deploymentId()).isEqualTo(12);
    }

    @Test
    @DisplayName("sans acteur — le pull d'un service — c'est la projection membre")
    void sansActeur() {
        assertThat(projection.project(serveur, null)).isInstanceOf(GameServerMemberDto.class);
    }

    @Test
    @DisplayName("un modérateur ne pilote que les serveurs dont il est référent")
    void moderateurReferent() {
        User moderateur = compte("modo-1");
        when(permissionEvaluator.can(eq(moderateur), eq(Permission.SERVER_START), isNull())).thenReturn(true);
        when(permissionEvaluator.can(eq(moderateur), eq(Permission.SERVER_STOP), isNull())).thenReturn(true);

        assertThat(((GameServerMemberDto) projection.project(serveur, moderateur)).viewerCanControl()).isFalse();

        serveur.setReferents(java.util.List.of("modo-1"));
        assertThat(((GameServerMemberDto) projection.project(serveur, moderateur)).viewerCanControl()).isTrue();
    }

    @Test
    @DisplayName("SERVER_CONTROL_ALL pilote sans être référent ; un référent sans rôle ne pilote rien")
    void controleGlobal() {
        User admin = compte("admin-1");
        when(permissionEvaluator.can(eq(admin), any(Permission.class), isNull())).thenReturn(true);
        assertThat(((GameServerDto) projection.project(serveur, admin)).viewerCanControl()).isTrue();

        User referentSansRole = compte("user-2");
        serveur.setReferents(java.util.List.of("user-2"));
        assertThat(((GameServerMemberDto) projection.project(serveur, referentSansRole)).viewerCanControl()).isFalse();
    }

    @Test
    @DisplayName("la projection membre ne porte aucun champ d'infrastructure")
    void membreSansInfrastructure() {
        assertThat(GameServerMemberDto.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("admins", "ports", "deploymentId");
    }
}
