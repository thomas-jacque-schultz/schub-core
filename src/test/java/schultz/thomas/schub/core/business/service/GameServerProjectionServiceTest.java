package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.GameServerMemberDto;
import schultz.thomas.schub.core.business.mapper.GameServerMapperImpl;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.data.model.Game;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Qui voit quoi d'un serveur.
 *
 * <p>La projection membre ne décrit pas l'installation : ni déploiement, ni ports. La garantie
 * est portée par le type — {@link GameServerMemberDto} n'a pas ces champs — et le dernier test
 * la verrouille, parce qu'un champ ajouté un jour ne lèverait aucune erreur.</p>
 */
class GameServerProjectionServiceTest {

    private PermissionEvaluator permissionEvaluator;
    private GameServerProjectionService projection;

    private GameServer serveur;

    @BeforeEach
    void setUp() {
        permissionEvaluator = mock(PermissionEvaluator.class);
        projection = new GameServerProjectionService(new GameServerMapperImpl(), permissionEvaluator);

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
    @DisplayName("la projection membre ne porte aucun champ d'infrastructure")
    void membreSansInfrastructure() {
        assertThat(GameServerMemberDto.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("admins", "ports", "deploymentId");
    }
}
