package schultz.thomas.schub.core.lol.team.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.SystemRole;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.lol.team.data.model.MacroPlan;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;
import schultz.thomas.schub.core.lol.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.lol.team.data.repository.MacroPlanRepository;
import schultz.thomas.schub.core.lol.team.data.repository.TeamChampionPoolRepository;
import schultz.thomas.schub.core.lol.team.data.repository.TeamRepository;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MacroPlanServiceTest {

    private static final List<Map<String, Object>> UN_TRAIT = List.of(Map.of("id", "t1", "type", "trait"));

    private MacroPlanRepository repository;
    private MacroPlanService service;

    private final User membre = compte("membre");
    private final User intrus = compte("intrus");

    @BeforeEach
    void setUp() {
        repository = mock(MacroPlanRepository.class);
        TeamRepository teamRepository = mock(TeamRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        GameServerService gameServerService = mock(GameServerService.class);
        when(gameServerService.findBySlug(anyString())).thenReturn(Optional.empty());

        Role visiteur = new Role();
        visiteur.setId("role-visiteur");
        visiteur.setName(SystemRole.VISITEUR.roleName());
        visiteur.setPermissions(EnumSet.copyOf(SystemRole.VISITEUR.permissions()));
        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(visiteur));

        PermissionEvaluator evaluator = new PermissionEvaluator(mock(UserRepository.class), roleRepository,
                List.of(new TeamScopedAuthority(teamRepository)));
        TeamService teamService = new TeamService(teamRepository, mock(CompositionRepository.class),
                mock(TeamChampionPoolRepository.class), repository, evaluator, mock(MemberDirectory.class),
                mock(RiotIdResolver.class));
        service = new MacroPlanService(repository, teamService, new ObjectMapper());

        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe()));
        when(repository.save(any(MacroPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("un membre crée un plan nommé, avec ses éléments")
    void unMembreCreeUnPlan() {
        MacroPlan plan = service.create(membre, "equipe-1", "  Setup Baron ", UN_TRAIT);

        assertThat(plan.getName()).isEqualTo("Setup Baron");
        assertThat(plan.getElements()).isEqualTo(UN_TRAIT);
        assertThat(plan.getUpdatedBy()).isEqualTo("membre");
    }

    @Test
    @DisplayName("hors de l'équipe, on ne crée ni ne lit de plan")
    void horsDeLEquipe() {
        assertThatThrownBy(() -> service.create(intrus, "equipe-1", "Espion", List.of()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.ofTeam(intrus, "equipe-1"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any(MacroPlan.class));
    }

    @Test
    @DisplayName("le plan d'une autre équipe ne se lit pas par celle-ci")
    void planDUneAutreEquipe() {
        MacroPlan ailleurs = new MacroPlan();
        ailleurs.setId("p-2");
        ailleurs.setTeamId("equipe-2");
        when(repository.findById("p-2")).thenReturn(Optional.of(ailleurs));

        assertThatThrownBy(() -> service.require(membre, "equipe-1", "p-2"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("enregistrer les éléments garde le nom, renommer garde les éléments")
    void miseAJourPartielle() {
        MacroPlan plan = service.create(membre, "equipe-1", "Early bleu", UN_TRAIT);
        plan.setId("p-1");
        when(repository.findById("p-1")).thenReturn(Optional.of(plan));

        service.update(membre, "equipe-1", "p-1", null, List.of());
        assertThat(plan.getName()).isEqualTo("Early bleu");
        assertThat(plan.getElements()).isEmpty();

        service.update(membre, "equipe-1", "p-1", "Early rouge", null);
        assertThat(plan.getName()).isEqualTo("Early rouge");
        assertThat(plan.getElements()).isEmpty();
    }

    @Test
    @DisplayName("un plan sans nom, ou trop lourd, est refusé")
    void limites() {
        assertThatThrownBy(() -> service.create(membre, "equipe-1", " ", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        List<Map<String, Object>> lourd = List.of(Map.of("texte", String.join("", Collections.nCopies(1_000_001, "x"))));
        assertThatThrownBy(() -> service.create(membre, "equipe-1", "Lourd", lourd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ko");
    }

    private static Team equipe() {
        Team team = new Team();
        team.setId("equipe-1");
        team.setName("Les cinq");
        team.setCreatedBy("capitaine");
        TeamMember member = new TeamMember();
        member.setMemberId("m-1");
        member.setUserId("membre");
        member.setStatus(MemberStatus.TITULAIRE);
        team.getMembers().add(member);
        return team;
    }

    private static User compte(String id) {
        User user = new User();
        user.setId(id);
        user.setDiscordId("discord-" + id);
        user.setRoleId("role-visiteur");
        return user;
    }
}
