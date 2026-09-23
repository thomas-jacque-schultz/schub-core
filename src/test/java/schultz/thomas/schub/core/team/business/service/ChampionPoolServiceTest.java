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
import schultz.thomas.schub.core.team.api.dto.ChampionPoolEntryDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChampionPoolServiceTest {

    private static final String PUUID_TOP = "puuid-top";
    private static final String PUUID_POLYVALENT = "puuid-polyvalent";
    private static final Instant RELEVE = Instant.parse("2026-09-21T06:00:00Z");

    private static final int JAX = 24;
    private static final int AHRI = 103;
    private static final int LEE_SIN = 64;

    private TeamRepository teamRepository;
    private RiotChampionGateway championGateway;
    private RiotStatsGateway statsGateway;
    private ChampionPoolService service;

    private User capitaine;
    private User membre;
    private User etranger;
    private Team equipe;
    private TeamChampionPool pool;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        championGateway = mock(RiotChampionGateway.class);
        statsGateway = mock(RiotStatsGateway.class);
        MemberDirectory memberDirectory = mock(MemberDirectory.class);
        TeamChampionPoolRepository pools = mock(TeamChampionPoolRepository.class);

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
                List.of(new TeamScopedAuthority(teamRepository)));
        TeamService teamService = new TeamService(teamRepository, mock(CompositionRepository.class),
                mock(TeamChampionPoolRepository.class),
                mock(GameReviewRepository.class), evaluator, memberDirectory,
                mock(RiotIdResolver.class));
        service = new ChampionPoolService(teamService, memberDirectory, championGateway, statsGateway, pools,
                evaluator);

        capitaine = compte("user-capitaine", "discord-capitaine");
        membre = compte("user-membre", "discord-membre");
        etranger = compte("user-etranger", "discord-etranger");

        equipe = new Team();
        equipe.setId("equipe-1");
        equipe.setName("Les Cinq");
        equipe.setCreatedBy(capitaine.getId());
        equipe.setMembers(new ArrayList<>(List.of(
                membreDe("m-top", capitaine.getId(), "Capitaine", PUUID_TOP,
                        List.of(GameRole.TOP), MemberStatus.TITULAIRE),
                membreDe("m-poly", membre.getId(), "Polyvalent", PUUID_POLYVALENT,
                        List.of(GameRole.TOP, GameRole.MID), MemberStatus.TITULAIRE),
                membreDe("m-libre", null, "SansCompte", null,
                        List.of(GameRole.TOP), MemberStatus.TITULAIRE))));
        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe));

        pool = new TeamChampionPool();
        pool.setTeamId("equipe-1");
        pool.setChampionKeys(GameRole.TOP, List.of("Jax"));
        when(pools.findById("equipe-1")).thenReturn(Optional.of(pool));
        when(pools.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(memberDirectory.byIds(any())).thenReturn(Map.of());
        when(championGateway.catalogue()).thenReturn(Optional.of(catalogue()));
        when(championGateway.masteries(anyString(), anyInt())).thenReturn(Optional.of(List.of()));
        when(championGateway.masteries(PUUID_TOP))
                .thenReturn(Optional.of(List.of(maitrise(JAX, 7, 250_000))));
        when(championGateway.masteries(PUUID_POLYVALENT))
                .thenReturn(Optional.of(List.of(maitrise(JAX, 4, 12_000), maitrise(AHRI, 6, 90_000))));
    }

    @Test
    @DisplayName("Un non-membre n'obtient pas le pool — refusé par la chaîne réelle")
    void refuseUnNonMembre() {
        assertThatThrownBy(() -> service.of(etranger, "equipe-1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Permission.TEAM_VIEW.name());

        verify(championGateway, never()).catalogue();
    }

    @Test
    @DisplayName("Un membre lit le pool mais ne l'écrit pas : viewerCanEdit le dit avant le refus")
    void unMembreLitSansEcrire() {
        assertThat(service.of(membre, "equipe-1", null).viewerCanEdit()).isFalse();
        assertThat(service.of(capitaine, "equipe-1", null).viewerCanEdit()).isTrue();

        assertThatThrownBy(() -> service.setChampions(membre, "equipe-1", GameRole.TOP, List.of("Jax")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Permission.COMPOSITION_EDIT.name());
    }

    @Test
    @DisplayName("Sous un champion retenu, ceux qui tiennent le poste, du plus maîtrisé au moins")
    void listeLesJoueursDuPosteParMaitrise() {
        ChampionPoolEntryDto jax = champion(service.of(capitaine, "equipe-1", null), GameRole.TOP, "Jax");

        assertThat(jax.name()).isEqualTo("Jax");
        assertThat(jax.players()).extracting("memberId").containsExactly("m-top", "m-poly");
        assertThat(jax.players()).extracting("masteryPoints").containsExactly(250_000, 12_000);
    }

    @Test
    @DisplayName("Chaque joueur porte ses parties et son taux de victoire sur le champion ; sans partie, on ne sait pas")
    void winrateParJoueur() {
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.CHAMPION), eq(RiotStatsGateway.Scope.RIFT), isNull()))
                .thenReturn(Optional.of(List.of(new RiotStatsGateway.Bucket(PUUID_TOP, String.valueOf(JAX), "Jax",
                        40, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 40 * 1800, null, null))));

        ChampionPoolEntryDto jax = champion(service.of(capitaine, "equipe-1", null), GameRole.TOP, "Jax");

        assertThat(jax.players()).extracting("games").containsExactly(40L, null);
        assertThat(jax.players().getFirst().winRate()).isEqualTo(0.65);
        assertThat(jax.players().get(1).winRate()).isNull();
    }

    @Test
    @DisplayName("Un membre à plusieurs postes apparaît dans chaque colonne qu'il tient")
    void unMembreCompteDansChacunDeSesPostes() {
        pool.setChampionKeys(GameRole.MID, List.of("Ahri"));

        ChampionPoolDto reponse = service.of(capitaine, "equipe-1", null);

        assertThat(champion(reponse, GameRole.TOP, "Jax").players())
                .extracting("memberId").contains("m-poly");
        assertThat(champion(reponse, GameRole.MID, "Ahri").players())
                .extracting("memberId").containsExactly("m-poly");
    }

    @Test
    @DisplayName("Un champion que personne ne maîtrise est rendu, avec une liste vide")
    void unChampionSansPersonneEstMasqueEtCompte() {
        pool.setChampionKeys(GameRole.TOP, List.of("Jax", "LeeSin"));

        ChampionPoolColumnDto top = colonne(service.of(capitaine, "equipe-1", null), GameRole.TOP);

        assertThat(top.champions()).extracting("championKey").doesNotContain("LeeSin");
        assertThat(top.hiddenByFloor()).isEqualTo(1);
        assertThat(pool.championKeys(GameRole.TOP)).contains("LeeSin");
    }

    @Test
    @DisplayName("Un membre sans compte Riot lié ne disparaît pas : il est dit au niveau du poste")
    void unMembreSansCompteRiotEstDit() {
        ChampionPoolColumnDto top = colonne(service.of(capitaine, "equipe-1", null), GameRole.TOP);

        assertThat(top.unavailableMembers()).extracting("memberId").containsExactly("m-libre");
        assertThat(top.unavailableMembers()).extracting("state")
                .containsExactly(PoolState.COMPTE_RIOT_ABSENT);
        assertThat(champion(service.of(capitaine, "equipe-1", null), GameRole.TOP, "Jax").players())
                .extracting("memberId").doesNotContain("m-libre");
    }

    @Test
    @DisplayName("Les champions d'une colonne sont triés par maîtrise moyenne décroissante")
    void trieParMaitriseMoyenne() {
        pool.setChampionKeys(GameRole.TOP, List.of("Jax", "Ahri"));
        when(championGateway.masteries(PUUID_TOP)).thenReturn(Optional.of(List.of(
                maitrise(JAX, 7, 10_000), maitrise(AHRI, 7, 400_000))));
        when(championGateway.masteries(PUUID_POLYVALENT)).thenReturn(Optional.of(List.of(
                maitrise(JAX, 4, 20_000), maitrise(AHRI, 6, 200_000))));

        ChampionPoolColumnDto top = colonne(service.of(capitaine, "equipe-1", null), GameRole.TOP);

        assertThat(top.champions()).extracting("championKey").containsExactly("Ahri", "Jax");
    }

    @Test
    @DisplayName("Le plancher masque le champion et dit combien")
    void lePlancherMasqueEtLeDit() {
        pool.setMasteryFloor(300_000);

        ChampionPoolColumnDto top = colonne(service.of(capitaine, "equipe-1", null), GameRole.TOP);

        assertThat(top.champions()).extracting("championKey").doesNotContain("Jax");
        assertThat(top.hiddenByFloor()).isPositive();
        assertThat(pool.championKeys(GameRole.TOP)).contains("Jax");
    }

    @Test
    @DisplayName("Un plancher demandé en lecture n'écrase pas celui de l'équipe")
    void leplancherDemandeNEcritRien() {
        pool.setMasteryFloor(1_000);

        ChampionPoolDto reponse = service.of(capitaine, "equipe-1", 100_000);

        assertThat(reponse.masteryFloor()).isEqualTo(100_000);
        assertThat(reponse.teamMasteryFloor()).isEqualTo(1_000);
        assertThat(champion(reponse, GameRole.TOP, "Jax").players())
                .extracting("memberId").containsExactly("m-top");
        assertThat(pool.getMasteryFloor()).isEqualTo(1_000);
    }

    @Test
    @DisplayName("Une clé de champion inconnue du patch est refusée plutôt qu'enregistrée")
    void refuseUneCleInconnue() {
        assertThatThrownBy(() -> service.setChampions(capitaine, "equipe-1", GameRole.MID,
                List.of("Jax", "ChampionQuiNExistePas")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChampionQuiNExistePas");
    }

    @Test
    @DisplayName("Sans catalogue, on n'écrit pas à l'aveugle")
    void refuseDEcrireSansCatalogue() {
        when(championGateway.catalogue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setChampions(capitaine, "equipe-1", GameRole.TOP, List.of("Jax")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Le même champion deux fois ne fait pas échouer : il est retenu une fois")
    void dedoublonneLaSelection() {
        ChampionPoolDto reponse =
                service.setChampions(capitaine, "equipe-1", GameRole.MID, List.of("Ahri", "Ahri"));

        assertThat(colonne(reponse, GameRole.MID).champions()).hasSize(1);
    }

    @Test
    @DisplayName("Un plancher négatif est refusé — il n'a pas de sens et masquerait une erreur d'appel")
    void refuseUnPlancherNegatif() {
        assertThatThrownBy(() -> service.setMasteryFloor(capitaine, "equipe-1", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Le catalogue entier accompagne le panneau, trié par nom")
    void sertLeCatalogueEntier() {
        ChampionPoolDto reponse = service.of(capitaine, "equipe-1", null);

        assertThat(reponse.patch()).isEqualTo("16.18.1");
        assertThat(reponse.catalog()).extracting("championKey")
                .containsExactly("Ahri", "Jax", "LeeSin");
    }

    @Test
    @DisplayName("Sans catalogue, aucun champion nulle part — et le choix n'est pas perdu")
    void sansCatalogueAucunChampion() {
        when(championGateway.catalogue()).thenReturn(Optional.empty());

        ChampionPoolDto reponse = service.of(capitaine, "equipe-1", null);

        assertThat(reponse.patch()).isNull();
        assertThat(reponse.catalog()).isEmpty();
        assertThat(colonne(reponse, GameRole.TOP).champions()).isEmpty();
        assertThat(colonne(reponse, GameRole.TOP).unavailableMembers()).hasSize(3);
        assertThat(pool.championKeys(GameRole.TOP)).containsExactly("Jax");
    }

    @Test
    @DisplayName("Un puuid, un seul appel de maîtrises, quel que soit le nombre de postes tenus")
    void neDemandeLesMaitrisesQuUneFoisParJoueur() {
        pool.setChampionKeys(GameRole.MID, List.of("Ahri"));

        service.of(capitaine, "equipe-1", null);

        verify(championGateway, times(1)).catalogue();
        verify(championGateway, times(1)).masteries(PUUID_POLYVALENT);
    }

    private static ChampionPoolColumnDto colonne(ChampionPoolDto pool, GameRole role) {
        return pool.columns().stream()
                .filter(colonne -> colonne.role() == role)
                .findFirst()
                .orElseThrow(() -> new AssertionError("aucune colonne au poste " + role));
    }

    private static ChampionPoolEntryDto champion(ChampionPoolDto pool, GameRole role, String key) {
        return colonne(pool, role).champions().stream()
                .filter(entree -> key.equals(entree.championKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("aucun champion " + key + " au poste " + role));
    }

    private static RiotChampionGateway.Catalogue catalogue() {
        Map<Integer, RiotChampionGateway.Champion> parId = new LinkedHashMap<>();
        parId.put(JAX, champion(JAX, "Jax", "Jax"));
        parId.put(AHRI, champion(AHRI, "Ahri", "Ahri"));
        parId.put(LEE_SIN, champion(LEE_SIN, "LeeSin", "Lee Sin"));
        return new RiotChampionGateway.Catalogue("16.18.1", parId);
    }

    private static RiotChampionGateway.Champion champion(int id, String key, String name) {
        return new RiotChampionGateway.Champion(id, key, name,
                "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/" + key + ".png");
    }

    private static RiotChampionGateway.Mastery maitrise(int championId, int level, int points) {
        return new RiotChampionGateway.Mastery(
                championId, level, points, Instant.parse("2026-09-20T20:00:00Z"), RELEVE);
    }

    private static TeamMember membreDe(String memberId, String userId, String gameName,
                                       String puuid, List<GameRole> roles, MemberStatus status) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(memberId);
        membre.setUserId(userId);
        membre.setRiotGameName(gameName);
        membre.setRiotTagLine("EUW");
        membre.setRiotPuuid(puuid);
        membre.setRoles(new ArrayList<>(roles));
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
