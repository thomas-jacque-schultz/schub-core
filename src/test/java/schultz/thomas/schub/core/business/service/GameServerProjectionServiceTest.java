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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le correctif d'identifiant, verrouillé par des tests.
 *
 * <p>Ce qui se cassait en silence : {@code GameServer.admins} contient des <strong>ids
 * internes</strong>, alors que l'acteur circule par son identifiant <strong>Discord</strong>.
 * Comparer les deux ne lève aucune erreur — ça répond simplement « non » à chaque fois, donc
 * personne n'est jamais reconnu comme administrateur de son serveur et la décision n°11 devient
 * décorative. Le troisième test est là spécifiquement pour ça.</p>
 *
 * <p>L'autre moitié du sujet est la fuite : la projection membre ne doit rien apprendre sur les
 * <em>autres</em> administrateurs. C'est ce que garantit le type — {@link GameServerMemberDto}
 * n'a pas de champ {@code admins} — et ce que ces tests documentent.</p>
 */
class GameServerProjectionServiceTest {

    private static final String SERVEUR_SLUG = "minecraft-1";
    private static final String ADMIN_ID_INTERNE = "user-admin";
    private static final String ADMIN_DISCORD_ID = "227883780512153610";

    private PermissionEvaluator permissionEvaluator;
    private UserService userService;
    private GameServerProjectionService projection;

    private GameServer serveur;

    @BeforeEach
    void setUp() {
        permissionEvaluator = mock(PermissionEvaluator.class);
        userService = mock(UserService.class);
        projection = new GameServerProjectionService(new GameServerMapperImpl(), permissionEvaluator, userService);

        serveur = new GameServer();
        serveur.setId("srv-1");
        serveur.setSlug(SERVEUR_SLUG);
        serveur.setName("Le serveur");
        serveur.setGame(Game.values()[0]);
        serveur.setDeploymentId(12);
        serveur.setAdmins(List.of(ADMIN_ID_INTERNE));

        when(userService.byIds(anySet())).thenReturn(Map.of());
    }

    private User compte(String idInterne, String discordId) {
        User user = new User();
        user.setId(idInterne);
        user.setDiscordId(discordId);
        return user;
    }

    private void sansVueInfra() {
        when(permissionEvaluator.can(any(User.class), eq(Permission.SERVER_INFRA_VIEW), isNull())).thenReturn(false);
    }

    private void avecVueInfra() {
        when(permissionEvaluator.can(any(User.class), eq(Permission.SERVER_INFRA_VIEW), isNull())).thenReturn(true);
    }

    @Test
    @DisplayName("un membre qui figure dans les admins le sait, sans voir la liste")
    void membreAdministrateur() {
        sansVueInfra();

        Object projete = projection.project(serveur, compte(ADMIN_ID_INTERNE, ADMIN_DISCORD_ID));

        assertThat(projete).isInstanceOf(GameServerMemberDto.class);
        assertThat(((GameServerMemberDto) projete).viewerIsAdmin()).isTrue();
    }

    @Test
    @DisplayName("un membre qui ne figure pas dans les admins ne l'apprend pas autrement")
    void membreOrdinaire() {
        sansVueInfra();

        GameServerMemberDto dto = (GameServerMemberDto) projection.project(serveur, compte("user-autre", "999"));

        assertThat(dto.viewerIsAdmin()).isFalse();
        // La garantie anti-fuite est portée par le type lui-même : aucun accesseur ne donne les
        // administrateurs. Si quelqu'un ajoute un champ `admins` ici un jour, ce commentaire est
        // le seul avertissement qu'il recevra.
        assertThat(GameServerMemberDto.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("admins", "ports", "deploymentId");
    }

    @Test
    @DisplayName("le piège corrigé : un identifiant Discord qui ressemble à un id interne ne suffit pas")
    void identifiantDiscordNeVautPasIdInterne() {
        sansVueInfra();
        // L'acteur porte comme identifiant Discord la valeur qui figure dans `admins`. Si la
        // comparaison se faisait sur le discordId — ce que faisait /auth/me en remontant le
        // sujet du jeton — il passerait pour administrateur alors qu'il ne l'est pas.
        User imposteur = compte("user-autre", ADMIN_ID_INTERNE);

        GameServerMemberDto dto = (GameServerMemberDto) projection.project(serveur, imposteur);

        assertThat(dto.viewerIsAdmin()).isFalse();
    }

    @Test
    @DisplayName("la projection infra porte la liste des admins et le booléen du lecteur")
    void projectionInfra() {
        avecVueInfra();
        User admin = compte(ADMIN_ID_INTERNE, ADMIN_DISCORD_ID);
        when(userService.byIds(anySet())).thenReturn(Map.of(ADMIN_ID_INTERNE, admin));

        GameServerDto dto = (GameServerDto) projection.project(serveur, admin);

        assertThat(dto.viewerIsAdmin()).isTrue();
        assertThat(dto.admins()).singleElement()
                .satisfies(entry -> assertThat(entry.userId()).isEqualTo(ADMIN_ID_INTERNE));
        assertThat(dto.deploymentId()).isEqualTo(12);
    }

    @Test
    @DisplayName("sans acteur — le pull d'un service — personne n'est administrateur")
    void sansActeur() {
        GameServerMemberDto dto = (GameServerMemberDto) projection.project(serveur, null);

        assertThat(dto.viewerIsAdmin()).isFalse();
    }
}
