package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.api.dto.TeamDto;
import schultz.thomas.schub.core.team.api.dto.TeamMemberDto;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamProjectionServiceTest {

    private TeamRepository teamRepository;
    private MemberDirectory memberDirectory;
    private TeamProjectionService projection;

    private User capitaine;
    private User membre;
    private User owner;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        memberDirectory = mock(MemberDirectory.class);

        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        GameServerService gameServerService = mock(GameServerService.class);
        when(gameServerService.findBySlug(anyString())).thenReturn(Optional.empty());

        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(
                role("role-visiteur", SystemRole.VISITEUR.roleName(), SystemRole.VISITEUR.permissions())));
        when(roleRepository.findById("role-owner")).thenReturn(Optional.of(
                role("role-owner", SystemRole.OWNER.roleName(), SystemRole.OWNER.permissions())));

        PermissionEvaluator evaluator = new PermissionEvaluator(userRepository, roleRepository,
                List.of(new TeamScopedAuthority(teamRepository)));
        projection = new TeamProjectionService(evaluator, memberDirectory);

        capitaine = compte("capitaine", "role-visiteur");
        membre = compte("membre", "role-visiteur");
        owner = compte("owner", "role-owner");

        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe()));
        when(memberDirectory.byIds(any())).thenReturn(Map.of(
                "capitaine", new MemberDirectory.MemberIdentity(
                        "capitaine", "Capitaine", "avatar-capitaine", null, null, null),
                "membre", new MemberDirectory.MemberIdentity(
                        "membre", "Membre", "avatar-membre", null, null, null)));
    }

    @Test
    @DisplayName("le capitaine lit qu'il peut écrire, sans qu'on lui dise qui d'autre le peut")
    void leCapitaine() {
        TeamDto dto = projection.toDto(equipe(), capitaine);

        assertThat(dto.viewerCanEdit()).isTrue();
        assertThat(dto.viewerCanEditCompositions()).isTrue();
        assertThat(dto.viewerMemberId()).isEqualTo("m-cap");
    }

    @Test
    @DisplayName("un membre lit qu'il ne peut pas écrire — le front n'a rien à déduire")
    void unMembre() {
        TeamDto dto = projection.toDto(equipe(), membre);

        assertThat(dto.viewerCanEdit()).isFalse();
        assertThat(dto.viewerCanEditCompositions()).isFalse();
        assertThat(dto.viewerMemberId()).isEqualTo("m-1");
    }

    @Test
    @DisplayName("un OWNER lit vrai par son rôle, sans figurer dans l'équipe")
    void unOwner() {
        TeamDto dto = projection.toDto(equipe(), owner);

        assertThat(dto.viewerCanEdit()).isTrue();
        assertThat(dto.viewerMemberId()).isNull();
    }

    @Test
    @DisplayName("la projection d'un membre ne porte aucun identifiant de compte")
    void aucuneFuiteDIdentifiant() {
        Set<String> champs = Arrays.stream(TeamMemberDto.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(champs).doesNotContain("userId", "discordId", "createdBy");
        assertThat(Arrays.stream(TeamDto.class.getRecordComponents()).map(RecordComponent::getName).toList())
                .doesNotContain("createdBy", "admins", "editors");
    }

    @Test
    @DisplayName("un membre lié s'affiche par son pseudo, un membre libre par son Riot ID")
    void nomAffiche() {
        TeamDto dto = projection.toDto(equipe(), capitaine);

        assertThat(dto.members()).extracting(TeamMemberDto::displayName)
                .containsExactly("Capitaine", "Membre", "Bibi#EUW");
        assertThat(dto.members()).extracting(TeamMemberDto::linked)
                .containsExactly(true, true, false);
    }

    @Test
    @DisplayName("le capitaine est signalé sur sa place, pas dans une liste d'ayants droit")
    void leCapitaineEstSignale() {
        TeamDto dto = projection.toDto(equipe(), membre);

        assertThat(dto.members()).filteredOn(TeamMemberDto::captain)
                .extracting(TeamMemberDto::memberId).containsExactly("m-cap");
    }

    @Test
    @DisplayName("un résumé porte les mêmes faits — une liste d'équipes sans un appel par ligne")
    void leResume() {
        assertThat(projection.toSummary(equipe(), capitaine).viewerCanEdit()).isTrue();
        assertThat(projection.toSummary(equipe(), membre).viewerCanEdit()).isFalse();
        assertThat(projection.toSummary(equipe(), membre).memberCount()).isEqualTo(3);
    }

    private Team equipe() {
        Team team = new Team();
        team.setId("equipe-1");
        team.setName("Les cinq");
        team.setCreatedBy("capitaine");
        team.getMembers().add(membre("m-cap", "capitaine", "Cap", "EUW"));
        team.getMembers().add(membre("m-1", "membre", "Mbr", "EUW"));
        team.getMembers().add(membre("m-libre", null, "Bibi", "EUW"));
        return team;
    }

    private TeamMember membre(String memberId, String userId, String gameName, String tagLine) {
        TeamMember member = new TeamMember();
        member.setMemberId(memberId);
        member.setUserId(userId);
        member.setRiotGameName(gameName);
        member.setRiotTagLine(tagLine);
        member.setStatus(MemberStatus.TITULAIRE);
        return member;
    }

    private Role role(String id, String name, Set<Permission> permissions) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setPermissions(EnumSet.copyOf(permissions));
        return role;
    }

    private User compte(String id, String roleId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId("discord-" + id);
        user.setRoleId(roleId);
        return user;
    }
}
