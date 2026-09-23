package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.api.dto.PlayerStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamStatsServiceTest {

    private static final Instant QUAND = Instant.parse("2026-09-20T18:00:00Z");
    private static final List<String> CINQ =
            List.of("puuid-1", "puuid-2", "puuid-3", "puuid-4", "puuid-5");

    private TeamRepository teamRepository;
    private RiotStatsGateway statsGateway;
    private RiotChampionGateway championGateway;
    private RiotConnectorService riotConnector;
    private TeamPlayerStatsService joueurs;
    private TeamGamesStatsService parties;

    private User capitaine;
    private User etranger;
    private Team equipe;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        statsGateway = mock(RiotStatsGateway.class);
        championGateway = mock(RiotChampionGateway.class);
        riotConnector = mock(RiotConnectorService.class);
        MemberDirectory memberDirectory = mock(MemberDirectory.class);

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
        PlayerStatsService playerStats =
                new PlayerStatsService(statsGateway, championGateway, riotConnector);
        joueurs = new TeamPlayerStatsService(teamService, memberDirectory, playerStats);
        parties = new TeamGamesStatsService(teamService, memberDirectory, statsGateway,
                championGateway);

        capitaine = compte("user-capitaine", "discord-capitaine");
        etranger = compte("user-etranger", "discord-etranger");

        equipe = new Team();
        equipe.setId("equipe-1");
        equipe.setName("Les Cinq");
        equipe.setCreatedBy(capitaine.getId());
        equipe.setMembers(new ArrayList<>(List.of(
                membre("m-top", capitaine.getId(), "puuid-1", GameRole.TOP),
                membre("m-jgl", null, "puuid-2", GameRole.JGL),
                membre("m-mid", null, "puuid-3", GameRole.MID),
                membre("m-adc", null, "puuid-4", GameRole.ADC),
                membre("m-sup", null, "puuid-5", GameRole.SUP))));
        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe));

        when(memberDirectory.byIds(any())).thenReturn(Map.of());
        when(championGateway.catalogue()).thenReturn(Optional.empty());
        when(riotConnector.ingestOf(anyString())).thenReturn(Optional.empty());
        when(statsGateway.aggregate(any(), any(), any(), any())).thenReturn(Optional.of(List.of()));
        when(statsGateway.rankings(anyString())).thenReturn(Optional.of(List.of()));
        when(statsGateway.coverage(any())).thenAnswer(invocation -> {
            List<String> puuids = invocation.getArgument(0);
            return couverture(puuids, 900, 900);
        });
        when(statsGateway.sharedMatches(any(), anyInt(), any(), any()))
                .thenReturn(Optional.of(new RiotStatsGateway.SharedMatches(4, 0, false, List.of())));
    }

    @Test
    @DisplayName("Un étranger n'obtient ni le panneau joueurs ni le panneau équipe")
    void refuseUnEtranger() {
        assertThatThrownBy(() -> joueurs.of(etranger, "equipe-1", null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Permission.TEAM_VIEW.name());
        assertThatThrownBy(() -> parties.of(etranger, "equipe-1", null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Permission.TEAM_VIEW.name());

        verify(statsGateway, never()).aggregate(any(), any(), any(), any());
        verify(statsGateway, never()).sharedMatches(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Un membre sans compte Riot ne fait pas échouer la requête : il dit pourquoi il est vide")
    void membreSansCompteRiot() {
        equipe.getMembers().get(2).setRiotPuuid(null);

        TeamPlayersStatsDto panneau = joueurs.of(capitaine, "equipe-1", null, null);

        assertThat(panneau.players()).hasSize(5);
        assertThat(colonne(panneau, "m-mid").state()).isEqualTo(StatsState.COMPTE_RIOT_ABSENT);
        assertThat(colonne(panneau, "m-mid").coverage()).isNull();
        assertThat(colonne(panneau, "m-mid").champions()).isEmpty();
    }

    @Test
    @DisplayName("Les colonnes sortent dans l'ordre des postes")
    void ordreDesPostes() {
        TeamPlayersStatsDto panneau = joueurs.of(capitaine, "equipe-1", null, null);

        assertThat(panneau.players()).extracting(PlayerStatsDto::roles)
                .containsExactly(List.of(GameRole.TOP), List.of(GameRole.JGL), List.of(GameRole.MID),
                        List.of(GameRole.ADC), List.of(GameRole.SUP));
    }

    @Test
    @DisplayName("Aucun puuid ne sort du cœur : un identifiant de membre suffit à l'écran")
    void aucunPuuidDansLaReponse() {
        String rendu = joueurs.of(capitaine, "equipe-1", null, null).toString();

        assertThat(rendu).doesNotContain("puuid-1").doesNotContain("puuid-5");
    }

    @Test
    @DisplayName("L'écart aux coéquipiers ne compte que ceux qui ont des parties")
    void ecartAuxCoequipiers() {
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.OVERALL), any(), any()))
                .thenReturn(Optional.of(List.of(
                        total("puuid-1", 100, 60),
                        total("puuid-2", 100, 40),
                        total("puuid-3", 100, 50))));

        TeamPlayersStatsDto panneau = joueurs.of(capitaine, "equipe-1", null, null);

        assertThat(colonne(panneau, "m-top").versusTeammates().comparedWith()).isEqualTo(2);
        assertThat(colonne(panneau, "m-top").versusTeammates().winRateDelta())
                .isEqualTo(0.6 - (0.4 + 0.5) / 2);
        assertThat(colonne(panneau, "m-adc").versusTeammates()).isNull();
    }

    @Test
    @DisplayName("Moins de quatre comptes liés : aucune partie d'équipe n'est possible, et on le dit")
    void effectifIncomplet() {
        equipe.getMembers().get(3).setRiotPuuid(null);
        equipe.getMembers().get(4).setRiotPuuid(null);

        TeamGamesStatsDto panneau = parties.of(capitaine, "equipe-1", null, null);

        assertThat(panneau.state()).isEqualTo(StatsState.EFFECTIF_INCOMPLET);
        assertThat(panneau.minimumPlayers()).isEqualTo(4);
        verify(statsGateway, never()).sharedMatches(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Aucune partie commune mais rien de collecté : l'ingestion n'est pas finie")
    void aucunePartieMaisIngestionEnCours() {
        when(statsGateway.coverage(any())).thenAnswer(invocation -> {
            List<String> puuids = invocation.getArgument(0);
            return couverture(puuids, 900, 0);
        });

        assertThat(parties.of(capitaine, "equipe-1", null, null).state())
                .isEqualTo(StatsState.INGESTION_EN_COURS);
    }

    @Test
    @DisplayName("Tout est collecté et il n'y a aucune partie à quatre : c'est une réponse")
    void aucunePartieCommune() {
        TeamGamesStatsDto panneau = parties.of(capitaine, "equipe-1", null, null);

        assertThat(panneau.state()).isEqualTo(StatsState.AUCUNE_PARTIE);
        assertThat(panneau.overall().games()).isZero();
        assertThat(panneau.overall().winRate()).isNull();
        assertThat(panneau.presence()).hasSize(5);
    }

    @Test
    @DisplayName("Le seuil est quatre, et la file n'exclut rien — elle est conservée et comptée à part")
    void toutesFilesConfondues() {
        when(statsGateway.sharedMatches(any(), anyInt(), any(), any())).thenReturn(Optional.of(
                new RiotStatsGateway.SharedMatches(4, 3, false, List.of(
                        partie("EUW1_1", 440, "RANKED_FLEX", true, 5),
                        partie("EUW1_2", 400, "NORMAL_DRAFT", false, 4),
                        partie("EUW1_3", 440, "RANKED_FLEX", true, 4)))));

        TeamGamesStatsDto panneau = parties.of(capitaine, "equipe-1", null, null);

        assertThat(panneau.overall().games()).isEqualTo(3);
        assertThat(panneau.overall().wins()).isEqualTo(2);
        assertThat(panneau.byQueue()).extracting(bilan -> bilan.key() + ":" + bilan.games())
                .containsExactly("RANKED_FLEX:2", "NORMAL_DRAFT:1");
        assertThat(panneau.games()).extracting(partie -> partie.queueId())
                .containsExactly(440, 400, 440);
        verify(statsGateway).sharedMatches(any(), eq(4), any(), any());
    }

    @Test
    @DisplayName("Membres répartis dans les deux camps : la partie compte, le résultat non")
    void campsOpposes() {
        RiotStatsGateway.SharedMatch separee = new RiotStatsGateway.SharedMatch("EUW1_9", QUAND,
                1800, 440, "RANKED_FLEX", "16.18", 4, true, null, List.of(
                joueur("puuid-1", true, 100), joueur("puuid-2", true, 100),
                joueur("puuid-3", false, 200), joueur("puuid-4", false, 200)), List.of());
        when(statsGateway.sharedMatches(any(), anyInt(), any(), any())).thenReturn(Optional.of(
                new RiotStatsGateway.SharedMatches(4, 1, false, List.of(separee))));

        TeamGamesStatsDto panneau = parties.of(capitaine, "equipe-1", null, null);

        assertThat(panneau.games()).singleElement()
                .satisfies(partie -> assertThat(partie.win()).isNull());
        assertThat(panneau.overall().games()).isZero();
        assertThat(panneau.undecidedGames()).isEqualTo(1);
    }

    @Test
    @DisplayName("Connecteur muet : le panneau le dit plutôt que d'annoncer zéro partie")
    void connecteurMuet() {
        when(statsGateway.sharedMatches(any(), anyInt(), any(), any())).thenReturn(Optional.empty());

        assertThat(parties.of(capitaine, "equipe-1", null, null).state())
                .isEqualTo(StatsState.CONNECTEUR_INDISPONIBLE);
    }

    private static Optional<List<RiotStatsGateway.Coverage>> couverture(List<String> puuids,
                                                                       long connues, long analysees) {
        if (puuids == null) {
            return Optional.of(List.of());
        }
        return Optional.of(puuids.stream()
                .map(puuid -> new RiotStatsGateway.Coverage(puuid, true, connues, analysees,
                        analysees == 0 ? null : QUAND, analysees == 0 ? null : QUAND, QUAND))
                .toList());
    }

    private static PlayerStatsDto colonne(TeamPlayersStatsDto panneau, String memberId) {
        return panneau.players().stream()
                .filter(colonne -> memberId.equals(colonne.memberId()))
                .findFirst()
                .orElseThrow();
    }

    private static RiotStatsGateway.Bucket total(String puuid, long games, long wins) {
        return new RiotStatsGateway.Bucket(puuid, "", null, games, wins, 100, 100, 100, 0, 0, 0, 0, 0,
                500, 400, 0, games * 1800, QUAND, QUAND);
    }

    private static RiotStatsGateway.SharedMatch partie(String matchId, int queueId, String queue,
                                                       boolean win, int presents) {
        List<RiotStatsGateway.SharedMatchPlayer> joueurs = CINQ.subList(0, presents).stream()
                .map(puuid -> joueur(puuid, win, 100))
                .toList();
        return new RiotStatsGateway.SharedMatch(matchId, QUAND, 1800, queueId, queue, "16.18",
                presents, false, win, joueurs, List.of());
    }

    private static RiotStatsGateway.SharedMatchPlayer joueur(String puuid, boolean win, int side) {
        return new RiotStatsGateway.SharedMatchPlayer(puuid, 126, "Jayce", "MIDDLE", win, side,
                5, 2, 3, 150, 12000, 20000, 18000, 25, false);
    }

    private static TeamMember membre(String memberId, String userId, String puuid, GameRole role) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(memberId);
        membre.setUserId(userId);
        membre.setRiotPuuid(puuid);
        membre.setRiotGameName("Joueur" + memberId);
        membre.setRiotTagLine("EUW");
        membre.setRoles(role == null ? List.of() : List.of(role));
        membre.setStatus(MemberStatus.TITULAIRE);
        return membre;
    }

    private static User compte(String id, String discordId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId(discordId);
        user.setRoleId("role-visiteur");
        return user;
    }

    @Test
    @DisplayName("Les patchs se rangent par version : 16.18 avant 16.9")
    void patchsParVersion() {
        assertThat(java.util.stream.Stream.of("16.9", "16.18", "15.24", "16.10")
                .sorted(((java.util.Comparator<String>) TeamGamesStatsService::parVersion).reversed())
                .toList()).containsExactly("16.18", "16.10", "16.9", "15.24");
    }
}
