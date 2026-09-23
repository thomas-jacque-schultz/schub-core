package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.api.dto.GameReviewDto;
import schultz.thomas.schub.core.team.api.dto.GameReviewsDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.data.model.GameReview;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameReviewServiceTest {

    private static final String PARTIE = "EUW1_7001";

    private GameReviewRepository reviewRepository;
    private TeamRepository teamRepository;
    private RiotStatsGateway statsGateway;
    private GameReviewService service;

    private User capitaine;
    private User membre;
    private User etranger;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(GameReviewRepository.class);
        teamRepository = mock(TeamRepository.class);
        statsGateway = mock(RiotStatsGateway.class);

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

        MemberDirectory annuaire = mock(MemberDirectory.class);
        when(annuaire.byIds(any())).thenReturn(Map.of());

        TeamService teamService = new TeamService(teamRepository, mock(CompositionRepository.class),
                mock(TeamChampionPoolRepository.class),
                reviewRepository, evaluator, annuaire, mock(RiotIdResolver.class));
        TeamGamesStatsService parties = new TeamGamesStatsService(teamService, annuaire,
                statsGateway, mock(RiotChampionGateway.class));

        service = new GameReviewService(reviewRepository, teamService, parties, annuaire, evaluator);

        capitaine = compte("capitaine");
        membre = compte("membre");
        etranger = compte("etranger");

        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe()));
        when(reviewRepository.save(any(GameReview.class))).thenAnswer(invocation -> {
            GameReview revue = invocation.getArgument(0);
            if (revue.getId() == null) {
                revue.setId("revue-1");
            }
            return revue;
        });
        when(reviewRepository.findByMatchIdAndSubjectMemberIdAndAuthorUserId(
                anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        partiesDEquipe(PARTIE);
    }

    @Test
    @DisplayName("le capitaine note n'importe quel joueur de son équipe")
    void capitaineNoteToutLeMonde() {
        GameReviewDto revue = service.create(capitaine, "equipe-1", PARTIE, "m-2", "Bon jeu bot");

        assertThat(revue.subjectMemberId()).isEqualTo("m-2");
        assertThat(revue.matchId()).isEqualTo(PARTIE);
        assertThat(revue.viewerCanEdit()).isTrue();
    }

    @Test
    @DisplayName("un membre écrit sur sa propre place")
    void membreNoteSaProprePlace() {
        GameReviewDto revue = service.create(membre, "equipe-1", PARTIE, "m-2", "J'ai mal tp");

        assertThat(revue.subjectMemberId()).isEqualTo("m-2");
        assertThat(revue.authorMemberId()).isEqualTo("m-2");
    }

    @Test
    @DisplayName("un membre n'écrit pas sur quelqu'un d'autre")
    void membreNeNotePasLesAutres() {
        assertThatThrownBy(() -> service.create(membre, "equipe-1", PARTIE, "m-3", "Mauvais jeu"))
                .isInstanceOf(AccessDeniedException.class);
        verify(reviewRepository, never()).save(any(GameReview.class));
    }

    @Test
    @DisplayName("un non-membre n'écrit rien, et ne lit rien")
    void etrangerRefuse() {
        assertThatThrownBy(() -> service.create(etranger, "equipe-1", PARTIE, "m-2", "Bonjour"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.ofGame(etranger, "equipe-1", PARTIE))
                .isInstanceOf(AccessDeniedException.class);
        verify(reviewRepository, never()).save(any(GameReview.class));
    }

    @Test
    @DisplayName("une partie qui n'est pas une partie d'équipe n'accueille aucune note")
    void refusePartieQuiNEstPasDEquipe() {
        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", "EUW1_9999", "m-2", "Note"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("n'est pas une partie de cette équipe");
        verify(reviewRepository, never()).save(any(GameReview.class));
    }

    @Test
    @DisplayName("une équipe sans partie d'équipe n'accueille aucune note")
    void equipeSansPartie() {
        partiesDEquipe();

        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-2", "Note"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("connecteur muet : on refuse au lieu de deviner")
    void connecteurMuet() {
        when(statsGateway.sharedMatches(any(), anyInt(), isNull(), anyInt()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-2", "Note"))
                .isInstanceOf(RiotConnectorUnavailableException.class);
        verify(reviewRepository, never()).save(any(GameReview.class));
    }

    @Test
    @DisplayName("un effectif de moins de quatre joueurs pourvus n'a aucune partie d'équipe")
    void effectifIncomplet() {
        Team maigre = equipe();
        maigre.getMembers().removeIf(membre -> !"m-2".equals(membre.getMemberId()));
        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(maigre));

        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-2", "Note"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("on ne note pas une place qui n'est pas de cette équipe")
    void sujetEtranger() {
        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-inconnu", "Note"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("on ne note pas un coach : il ne joue pas")
    void sujetCoach() {
        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-coach", "Note"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("une note vide n'est pas une note")
    void contenuVide() {
        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-2", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("une seule note par auteur, par joueur et par partie")
    void doublonRefuse() {
        when(reviewRepository.findByMatchIdAndSubjectMemberIdAndAuthorUserId(PARTIE, "m-2", "capitaine"))
                .thenReturn(Optional.of(revue("revue-0", "m-2", "capitaine")));

        assertThatThrownBy(() -> service.create(capitaine, "equipe-1", PARTIE, "m-2", "Encore"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("la note d'un autre ne se retouche pas, même par un membre")
    void retoucheRefusee() {
        when(reviewRepository.findById("revue-1"))
                .thenReturn(Optional.of(revue("revue-1", "m-3", "capitaine")));

        assertThatThrownBy(() -> service.update(membre, "equipe-1", PARTIE, "revue-1", "Changé"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("le capitaine retouche celle d'un autre ; l'auteur retouche la sienne")
    void retoucheAutorisee() {
        when(reviewRepository.findById("revue-1"))
                .thenReturn(Optional.of(revue("revue-1", "m-2", "membre")));

        assertThat(service.update(membre, "equipe-1", PARTIE, "revue-1", "Relu").content())
                .isEqualTo("Relu");
        assertThat(service.update(capitaine, "equipe-1", PARTIE, "revue-1", "Revu").content())
                .isEqualTo("Revu");
    }

    @Test
    @DisplayName("une note attachée à une autre partie n'est pas de cette partie")
    void noteDUneAutrePartie() {
        GameReview ailleurs = revue("revue-1", "m-2", "membre");
        ailleurs.setMatchId("EUW1_8888");
        when(reviewRepository.findById("revue-1")).thenReturn(Optional.of(ailleurs));

        assertThatThrownBy(() -> service.update(capitaine, "equipe-1", PARTIE, "revue-1", "X"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("la lecture porte ce que le lecteur peut écrire, et rien sur les autres")
    void faitsSurLeLecteur() {
        when(reviewRepository.findByTeamIdAndMatchIdOrderByCreatedAtAsc("equipe-1", PARTIE))
                .thenReturn(List.of(revue("revue-1", "m-2", "membre")));

        GameReviewsDto vuParLeMembre = service.ofGame(membre, "equipe-1", PARTIE);
        assertThat(vuParLeMembre.viewerCanReviewAnyone()).isFalse();
        assertThat(vuParLeMembre.viewerMemberId()).isEqualTo("m-2");
        assertThat(vuParLeMembre.reviews()).singleElement()
                .extracting(GameReviewDto::viewerCanEdit).isEqualTo(true);

        GameReviewsDto vuParLeCapitaine = service.ofGame(capitaine, "equipe-1", PARTIE);
        assertThat(vuParLeCapitaine.viewerCanReviewAnyone()).isTrue();
        assertThat(vuParLeCapitaine.viewerMemberId()).isNull();
    }

    private void partiesDEquipe(String... matchIds) {
        List<RiotStatsGateway.SharedMatch> parties = java.util.Arrays.stream(matchIds)
                .map(id -> new RiotStatsGateway.SharedMatch(id, Instant.now(), 1800, 440, "flex",
                        "14.18", 5, false, true, List.of()))
                .toList();
        when(statsGateway.sharedMatches(any(), eq(4), isNull(), anyInt()))
                .thenReturn(Optional.of(new RiotStatsGateway.SharedMatches(4, parties.size(), false, parties)));
    }

    private GameReview revue(String id, String subjectMemberId, String authorUserId) {
        GameReview revue = new GameReview();
        revue.setId(id);
        revue.setTeamId("equipe-1");
        revue.setMatchId(PARTIE);
        revue.setSubjectMemberId(subjectMemberId);
        revue.setAuthorUserId(authorUserId);
        revue.setContent("Note");
        revue.setCreatedAt(Instant.now());
        revue.setUpdatedAt(revue.getCreatedAt());
        return revue;
    }

    private Team equipe() {
        Team team = new Team();
        team.setId("equipe-1");
        team.setName("Les cinq");
        team.setCreatedBy("capitaine");
        team.getMembers().add(membre("m-1", null, "puuid-1", GameRole.TOP, MemberStatus.TITULAIRE));
        team.getMembers().add(membre("m-2", "membre", "puuid-2", GameRole.JGL, MemberStatus.TITULAIRE));
        team.getMembers().add(membre("m-3", null, "puuid-3", GameRole.MID, MemberStatus.TITULAIRE));
        team.getMembers().add(membre("m-4", null, "puuid-4", GameRole.ADC, MemberStatus.TITULAIRE));
        team.getMembers().add(membre("m-5", null, "puuid-5", GameRole.SUP, MemberStatus.TITULAIRE));
        team.getMembers().add(membre("m-coach", null, null, null, MemberStatus.COACH));
        return team;
    }

    private TeamMember membre(String memberId, String userId, String puuid, GameRole role,
                              MemberStatus status) {
        TeamMember member = new TeamMember();
        member.setMemberId(memberId);
        member.setUserId(userId);
        member.setRiotPuuid(puuid);
        member.setRiotGameName(memberId);
        member.setRiotTagLine("EUW");
        member.setRoles(role == null ? List.of() : List.of(role));
        member.setStatus(status);
        return member;
    }

    private User compte(String id) {
        User user = new User();
        user.setId(id);
        user.setDiscordId("discord-" + id);
        user.setRoleId("role-visiteur");
        return user;
    }
}
