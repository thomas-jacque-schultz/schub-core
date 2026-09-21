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
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.data.model.Composition;
import schultz.thomas.schub.core.team.data.model.CompositionSlot;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * « Une composition en désigne exactement cinq. »
 *
 * <p>C'est la seule contrainte de cardinalité du domaine, et elle ne remonte pas à l'équipe. Ce
 * qui est vérifié ici, ce sont les formes fausses qui passeraient sans bruit : quatre postes, six
 * postes, deux fois le même poste, deux fois le même joueur, un joueur qui n'est pas de l'équipe.
 * Aucune ne lève d'erreur à l'écriture si personne ne la refuse — elle dort en base jusqu'à ce
 * qu'on rouvre la composition avant une partie.</p>
 */
class CompositionServiceTest {

    private CompositionRepository compositionRepository;
    private TeamRepository teamRepository;
    private CompositionService compositionService;

    private User capitaine;
    private User membre;

    @BeforeEach
    void setUp() {
        compositionRepository = mock(CompositionRepository.class);
        teamRepository = mock(TeamRepository.class);

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

        TeamService teamService = new TeamService(teamRepository, compositionRepository,
                mock(GameReviewRepository.class), evaluator, mock(MemberDirectory.class),
                mock(RiotIdResolver.class));
        compositionService = new CompositionService(compositionRepository, teamService, evaluator);

        capitaine = compte("capitaine");
        membre = compte("membre");

        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe()));
        when(compositionRepository.save(any(Composition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("cinq postes distincts, chacun avec son champion : c'est une composition")
    void compositionValide() {
        Composition composition = compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Contre poke", cinqPostes(), "14.18.1", "Prio bot"));

        assertThat(composition.getSlots()).hasSize(5);
        assertThat(composition.getPatch()).isEqualTo("14.18.1");
        assertThat(composition.getCreatedBy()).isEqualTo("capitaine");
    }

    @Test
    @DisplayName("quatre postes, ce n'est pas une équipe sur la Faille")
    void refuseQuatrePostes() {
        List<CompositionSlot> quatre = new ArrayList<>(cinqPostes());
        quatre.remove(4);

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Bancale", quatre, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactement 5");
        verify(compositionRepository, never()).save(any(Composition.class));
    }

    @Test
    @DisplayName("six postes non plus")
    void refuseSixPostes() {
        List<CompositionSlot> six = new ArrayList<>(cinqPostes());
        six.add(new CompositionSlot(GameRole.MID, "Zed", null));

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Trop", six, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactement 5");
    }

    @Test
    @DisplayName("cinq lignes mais deux fois le même poste : refusé — la taille ne suffit pas")
    void refuseUnPosteEnDouble() {
        List<CompositionSlot> doublon = new ArrayList<>(cinqPostes());
        doublon.set(4, new CompositionSlot(GameRole.MID, "Zed", null));

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Deux mids", doublon, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deux fois");
    }

    @Test
    @DisplayName("un poste sans champion est un trou, pas une proposition")
    void refuseUnPosteSansChampion() {
        List<CompositionSlot> troue = new ArrayList<>(cinqPostes());
        troue.set(2, new CompositionSlot(GameRole.MID, "  ", null));

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Trouée", troue, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sans champion");
    }

    @Test
    @DisplayName("un même joueur ne tient pas deux postes")
    void refuseUnJoueurEnDouble() {
        List<CompositionSlot> slots = new ArrayList<>(cinqPostes());
        slots.set(0, new CompositionSlot(GameRole.TOP, "Ornn", "m-1"));
        slots.set(1, new CompositionSlot(GameRole.JGL, "Sejuani", "m-1"));

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Dédoublé", slots, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deux postes");
    }

    @Test
    @DisplayName("on ne désigne pas quelqu'un qui n'est pas de l'équipe")
    void refuseUnJoueurEtranger() {
        List<CompositionSlot> slots = new ArrayList<>(cinqPostes());
        slots.set(0, new CompositionSlot(GameRole.TOP, "Ornn", "m-inconnu"));

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Invité", slots, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'est pas un joueur de cette équipe");
    }

    @Test
    @DisplayName("le coach n'est pas retenu dans une composition — il ne joue pas")
    void refuseLeCoach() {
        List<CompositionSlot> slots = new ArrayList<>(cinqPostes());
        slots.set(0, new CompositionSlot(GameRole.TOP, "Ornn", "m-coach"));

        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Coach au top", slots, "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'est pas un joueur de cette équipe");
    }

    @Test
    @DisplayName("un poste sans joueur passe : une composition se prépare avant que l'effectif soit complet")
    void tolereUnPosteSansJoueur() {
        List<CompositionSlot> slots = new ArrayList<>(cinqPostes());
        slots.set(0, new CompositionSlot(GameRole.TOP, "Ornn", "m-1"));

        Composition composition = compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft("Brouillon", slots, "14.18.1", null));

        assertThat(composition.getSlots()).extracting(CompositionSlot::getMemberId)
                .containsExactly("m-1", null, null, null, null);
    }

    @Test
    @DisplayName("un membre ne peut pas écrire de composition — il voit tout et n'écrit rien")
    void unMembreNEcritPas() {
        assertThatThrownBy(() -> compositionService.create(membre, "equipe-1",
                new CompositionService.Draft("La mienne", cinqPostes(), "14.18.1", null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("COMPOSITION_EDIT");
        verify(compositionRepository, never()).save(any(Composition.class));
    }

    @Test
    @DisplayName("un membre lit les compositions de son équipe")
    void unMembreLit() {
        when(compositionRepository.findByTeamIdOrderByUpdatedAtDesc("equipe-1")).thenReturn(List.of());

        assertThat(compositionService.ofTeam(membre, "equipe-1")).isEmpty();
    }

    @Test
    @DisplayName("une composition d'une autre équipe n'est pas servie par cette équipe-ci")
    void refuseUneCompositionDUneAutreEquipe() {
        Composition ailleurs = new Composition();
        ailleurs.setId("compo-9");
        ailleurs.setTeamId("equipe-2");
        when(compositionRepository.findById("compo-9")).thenReturn(Optional.of(ailleurs));

        assertThatThrownBy(() -> compositionService.require(capitaine, "equipe-1", "compo-9"))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("n'appartient pas");
    }

    @Test
    @DisplayName("une composition a un nom")
    void nomObligatoire() {
        assertThatThrownBy(() -> compositionService.create(capitaine, "equipe-1",
                new CompositionService.Draft(" ", cinqPostes(), "14.18.1", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- montage ---

    private List<CompositionSlot> cinqPostes() {
        return new ArrayList<>(List.of(
                new CompositionSlot(GameRole.TOP, "Ornn", null),
                new CompositionSlot(GameRole.JGL, "Sejuani", null),
                new CompositionSlot(GameRole.MID, "Orianna", null),
                new CompositionSlot(GameRole.ADC, "Jinx", null),
                new CompositionSlot(GameRole.SUP, "Thresh", null)));
    }

    private Team equipe() {
        Team team = new Team();
        team.setId("equipe-1");
        team.setName("Les cinq");
        team.setCreatedBy("capitaine");
        team.getMembers().add(membre("m-1", "membre", MemberStatus.TITULAIRE));
        team.getMembers().add(membre("m-coach", null, MemberStatus.COACH));
        return team;
    }

    private TeamMember membre(String memberId, String userId, MemberStatus status) {
        TeamMember member = new TeamMember();
        member.setMemberId(memberId);
        member.setUserId(userId);
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
