package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolColumnDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolMemberDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le panneau 3, branché sur le <em>vrai</em> évaluateur de permissions.
 *
 * <p>Comme {@code TeamServiceTest}, les mocks s'arrêtent aux dépôts et à la passerelle Riot :
 * l'autorisation passe pour de bon par {@link PermissionEvaluator} et
 * {@link TeamScopedAuthority}. Un {@code when(...).thenReturn(false)} ne prouverait pas qu'un
 * non-membre est refusé par la chaîne réelle.</p>
 */
class ChampionPoolServiceTest {

    private static final String PUUID_CAPITAINE = "puuid-capitaine";
    private static final Instant RELEVE = Instant.parse("2026-09-21T06:00:00Z");

    private TeamRepository teamRepository;
    private RiotChampionGateway championGateway;
    private MemberDirectory memberDirectory;
    private ChampionPoolService service;

    private User capitaine;
    private User membre;
    private User etranger;
    private Team equipe;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        championGateway = mock(RiotChampionGateway.class);
        memberDirectory = mock(MemberDirectory.class);

        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        GameServerService gameServerService = mock(GameServerService.class);
        when(gameServerService.findBySlug(anyString())).thenReturn(Optional.empty());

        Role visiteur = new Role();
        visiteur.setId("role-visiteur");
        visiteur.setName(SystemRole.VISITEUR.roleName());
        visiteur.setPermissions(EnumSet.copyOf(SystemRole.VISITEUR.permissions()));
        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(visiteur));

        PermissionEvaluator evaluator = new PermissionEvaluator(userRepository, roleRepository,
                gameServerService, List.of(new TeamScopedAuthority(teamRepository)));
        TeamService teamService = new TeamService(teamRepository, mock(CompositionRepository.class),
                evaluator, memberDirectory, mock(RiotIdResolver.class));
        service = new ChampionPoolService(teamService, memberDirectory, championGateway);

        capitaine = compte("user-capitaine", "discord-capitaine");
        membre = compte("user-membre", "discord-membre");
        etranger = compte("user-etranger", "discord-etranger");

        equipe = new Team();
        equipe.setId("equipe-1");
        equipe.setName("Les Cinq");
        equipe.setCreatedBy(capitaine.getId());
        equipe.setMembers(new java.util.ArrayList<>(List.of(
                membreDe("m-top", capitaine.getId(), "Capitaine", "EUW", PUUID_CAPITAINE,
                        GameRole.TOP, MemberStatus.TITULAIRE),
                membreDe("m-mid", membre.getId(), "Milieu", "EUW", null,
                        GameRole.MID, MemberStatus.TITULAIRE))));
        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe));

        when(memberDirectory.byIds(any())).thenReturn(Map.of());
        when(championGateway.catalogue()).thenReturn(Optional.of(catalogue()));
        when(championGateway.masteries(anyString(), anyInt())).thenReturn(Optional.of(List.of()));
    }

    // --- l'autorisation ---

    @Test
    @DisplayName("Un non-membre n'obtient pas le pool — refusé par la chaîne réelle")
    void refuseUnNonMembre() {
        assertThatThrownBy(() -> service.of(etranger, "equipe-1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Permission.TEAM_VIEW.name());

        verify(championGateway, never()).catalogue();
        verify(championGateway, never()).masteries(anyString(), anyInt());
    }

    @Test
    @DisplayName("Un membre simple voit le pool : appartenir à l'équipe suffit")
    void autoriseUnMembre() {
        assertThat(service.of(membre, "equipe-1", null).teamName()).isEqualTo("Les Cinq");
    }

    // --- le patch, figé ---

    @Test
    @DisplayName("La version du patch est figée dans la réponse")
    void figeLaVersionDuPatch() {
        when(championGateway.masteries(PUUID_CAPITAINE, 10))
                .thenReturn(Optional.of(List.of(maitrise(24, 7, 250_000))));

        ChampionPoolDto pool = service.of(capitaine, "equipe-1", null);

        assertThat(pool.patch()).isEqualTo("16.18.1");
        assertThat(champions(pool, GameRole.TOP)).singleElement()
                .satisfies(entree -> {
                    assertThat(entree.championId()).isEqualTo(24);
                    assertThat(entree.championKey()).isEqualTo("Jax");
                    assertThat(entree.name()).isEqualTo("Jax");
                    assertThat(entree.iconUrl()).contains("16.18.1");
                    assertThat(entree.masteryLevel()).isEqualTo(7);
                });
    }

    @Test
    @DisplayName("Catalogue indisponible : aucun champion nulle part, et le patch est nul")
    void sansCatalogueAucunChampion() {
        when(championGateway.catalogue()).thenReturn(Optional.empty());
        when(championGateway.masteries(anyString(), anyInt()))
                .thenReturn(Optional.of(List.of(maitrise(24, 7, 250_000))));

        ChampionPoolDto pool = service.of(capitaine, "equipe-1", null);

        assertThat(pool.patch()).isNull();
        assertThat(membre(pool, GameRole.TOP).state()).isEqualTo(PoolState.CATALOGUE_INDISPONIBLE);
        assertThat(membre(pool, GameRole.TOP).champions()).isEmpty();
        verify(championGateway, never()).masteries(anyString(), anyInt());
    }

    @Test
    @DisplayName("Un champion absent du catalogue est rendu quand même, sans nom ni icône")
    void gardeUnChampionInconnuDuCatalogue() {
        when(championGateway.masteries(PUUID_CAPITAINE, 10))
                .thenReturn(Optional.of(List.of(maitrise(9999, 5, 40_000))));

        assertThat(champions(service.of(capitaine, "equipe-1", null), GameRole.TOP))
                .singleElement()
                .satisfies(entree -> {
                    assertThat(entree.championId()).isEqualTo(9999);
                    assertThat(entree.name()).isNull();
                    assertThat(entree.iconUrl()).isNull();
                    assertThat(entree.masteryPoints()).isEqualTo(40_000);
                });
    }

    // --- les membres qu'on ne perd pas ---

    @Test
    @DisplayName("Un membre sans puuid ne fait pas échouer la requête et ne disparaît pas")
    void unMembreSansPuuidEstRenduAvecSaRaison() {
        when(championGateway.masteries(PUUID_CAPITAINE, 10))
                .thenReturn(Optional.of(List.of(maitrise(24, 7, 250_000))));

        ChampionPoolDto pool = service.of(capitaine, "equipe-1", null);

        ChampionPoolMemberDto milieu = membre(pool, GameRole.MID);
        assertThat(milieu.state()).isEqualTo(PoolState.COMPTE_RIOT_ABSENT);
        assertThat(milieu.champions()).isEmpty();
        assertThat(milieu.displayName()).isEqualTo("Milieu#EUW");
        assertThat(membre(pool, GameRole.TOP).champions()).hasSize(1);
        verify(championGateway, never()).masteries(null, 10);
    }

    @Test
    @DisplayName("Connecteur injoignable sur les maîtrises : l'état le dit, il ne dit pas « aucune »")
    void distingueIndisponibleDeVide() {
        when(championGateway.masteries(PUUID_CAPITAINE, 10)).thenReturn(Optional.empty());

        assertThat(membre(service.of(capitaine, "equipe-1", null), GameRole.TOP).state())
                .isEqualTo(PoolState.MAITRISES_INDISPONIBLES);

        when(championGateway.masteries(PUUID_CAPITAINE, 10)).thenReturn(Optional.of(List.of()));

        assertThat(membre(service.of(capitaine, "equipe-1", null), GameRole.TOP).state())
                .isEqualTo(PoolState.AUCUNE_MAITRISE);
    }

    @Test
    @DisplayName("Les cinq colonnes sont toujours là, même vides")
    void rendToujoursLesCinqColonnes() {
        ChampionPoolDto pool = service.of(capitaine, "equipe-1", null);

        assertThat(pool.columns()).hasSize(5);
        assertThat(pool.columns()).extracting(ChampionPoolColumnDto::role)
                .containsExactly(GameRole.TOP, GameRole.JGL, GameRole.MID, GameRole.ADC, GameRole.SUP);
        assertThat(pool.columns().get(1).members()).isEmpty();
    }

    @Test
    @DisplayName("Un membre sans poste n'est dans aucune colonne, mais il est rendu")
    void rendLesMembresSansPoste() {
        equipe.getMembers().add(membreDe("m-libre", null, "Polyvalent", "EUW", null,
                null, MemberStatus.REMPLACANT));

        ChampionPoolDto pool = service.of(capitaine, "equipe-1", null);

        assertThat(pool.membersWithoutRole()).extracting(ChampionPoolMemberDto::memberId)
                .containsExactly("m-libre");
        assertThat(pool.columns()).allSatisfy(colonne ->
                assertThat(colonne.members()).noneMatch(m -> "m-libre".equals(m.memberId())));
    }

    @Test
    @DisplayName("Un coach est hors du panneau : il ne tient pas de poste et n'aligne rien")
    void ecarteLesCoachs() {
        equipe.getMembers().add(membreDe("m-coach", null, "Coach", "EUW", "puuid-coach",
                null, MemberStatus.COACH));

        ChampionPoolDto pool = service.of(capitaine, "equipe-1", null);

        assertThat(pool.membersWithoutRole()).extracting(ChampionPoolMemberDto::memberId)
                .doesNotContain("m-coach");
        verify(championGateway, never()).masteries("puuid-coach", 10);
    }

    // --- la borne ---

    @Test
    @DisplayName("Le nombre de champions demandé est borné, et la valeur appliquée est rendue")
    void borneLaDemande() {
        assertThat(service.of(capitaine, "equipe-1", null).championsPerMember()).isEqualTo(10);
        assertThat(service.of(capitaine, "equipe-1", 3).championsPerMember()).isEqualTo(3);
        assertThat(service.of(capitaine, "equipe-1", 500).championsPerMember())
                .isEqualTo(ChampionPoolService.CHAMPIONS_PAR_MEMBRE_MAX);
        assertThat(service.of(capitaine, "equipe-1", -1).championsPerMember()).isEqualTo(1);
    }

    @Test
    @DisplayName("Le relevé Riot est daté par le connecteur, pas par l'instant de la requête")
    void porteLaDateDuReleve() {
        when(championGateway.masteries(PUUID_CAPITAINE, 10))
                .thenReturn(Optional.of(List.of(maitrise(24, 7, 250_000))));

        assertThat(membre(service.of(capitaine, "equipe-1", null), GameRole.TOP).observedAt())
                .isEqualTo(RELEVE);
    }

    @Test
    @DisplayName("Le lecteur retrouve sa propre place sans comparer d'identifiants")
    void donneLaPlaceDuLecteur() {
        assertThat(service.of(membre, "equipe-1", null).viewerMemberId()).isEqualTo("m-mid");
        assertThat(service.of(capitaine, "equipe-1", null).viewerMemberId()).isEqualTo("m-top");
    }

    @Test
    @DisplayName("Deux membres, un seul appel de catalogue")
    void neDemandeLeCatalogueQuUneFois() {
        service.of(capitaine, "equipe-1", null);

        verify(championGateway, times(1)).catalogue();
    }

    // --- outillage ---

    private static ChampionPoolMemberDto membre(ChampionPoolDto pool, GameRole role) {
        return pool.columns().stream()
                .filter(colonne -> colonne.role() == role)
                .flatMap(colonne -> colonne.members().stream())
                .findFirst()
                .orElseThrow(() -> new AssertionError("aucun membre au poste " + role));
    }

    private static List<schultz.thomas.schub.core.team.api.dto.ChampionPoolEntryDto> champions(
            ChampionPoolDto pool, GameRole role) {
        return membre(pool, role).champions();
    }

    private static RiotChampionGateway.Catalogue catalogue() {
        return new RiotChampionGateway.Catalogue("16.18.1", Map.of(
                24, new RiotChampionGateway.Champion(24, "Jax", "Jax",
                        "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/Jax.png")));
    }

    private static RiotChampionGateway.Mastery maitrise(int championId, int level, int points) {
        return new RiotChampionGateway.Mastery(
                championId, level, points, Instant.parse("2026-09-20T20:00:00Z"), RELEVE);
    }

    private static TeamMember membreDe(String memberId, String userId, String gameName, String tagLine,
                                       String puuid, GameRole role, MemberStatus status) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(memberId);
        membre.setUserId(userId);
        membre.setRiotGameName(gameName);
        membre.setRiotTagLine(tagLine);
        membre.setRiotPuuid(puuid);
        membre.setRole(role);
        membre.setStatus(status);
        return membre;
    }

    private static User compte(String id, String discordId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId(discordId);
        user.setRoleId("role-visiteur");
        return user;
    }
}
