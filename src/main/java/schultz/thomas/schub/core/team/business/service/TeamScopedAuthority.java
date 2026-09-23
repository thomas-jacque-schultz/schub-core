package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.business.service.ScopedAuthorityProvider;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.util.Set;

/**
 * créateur -> TEAM_VIEW, TEAM_EDIT, COMPOSITION_EDIT
 * membre   -> TEAM_VIEW
 * le reste -> rien (OWNER passe par son rôle)
 */
@Service
@RequiredArgsConstructor
public class TeamScopedAuthority implements ScopedAuthorityProvider {

    private static final Set<Permission> DU_CAPITAINE =
            Set.of(Permission.TEAM_VIEW, Permission.TEAM_EDIT, Permission.COMPOSITION_EDIT);

    private static final Set<Permission> DU_MEMBRE = Set.of(Permission.TEAM_VIEW);

    private final TeamRepository teamRepository;

    @Override
    public ResourceType resourceType() {
        return ResourceType.TEAM;
    }

    @Override
    public Set<Permission> grantedTo(User actor, String teamId) {
        if (actor == null || actor.getId() == null || teamId == null) {
            return Set.of();
        }
        return teamRepository.findById(teamId)
                .map(team -> grantedTo(actor, team))
                .orElseGet(Set::of);
    }

    private Set<Permission> grantedTo(User actor, Team team) {
        if (actor.getId().equals(team.getCreatedBy())) {
            return DU_CAPITAINE;
        }
        return team.hasMemberLinkedTo(actor.getId()) ? DU_MEMBRE : Set.of();
    }
}
