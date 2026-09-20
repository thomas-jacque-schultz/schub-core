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
 * Ce qu'une équipe donne à qui lui appartient.
 *
 * <pre>
 *   créateur  ->  TEAM_VIEW, TEAM_EDIT, COMPOSITION_EDIT
 *   membre    ->  TEAM_VIEW
 *   le reste  ->  rien
 * </pre>
 *
 * <p>C'est la mécanique de portée du plan §A.1, telle quelle : appartenir à une ressource donne
 * des droits sur <em>cette</em> ressource, exactement comme figurer dans les {@code admins} d'un
 * serveur donne {@code SERVER_START} sur celui-là. Rien n'est dupliqué de
 * {@link schultz.thomas.schub.core.business.service.PermissionEvaluator} : il pose la question,
 * cette classe répond pour son domaine.</p>
 *
 * <p><strong>Un membre voit tout et n'écrit rien</strong> (plan §D.2 bis, point 2). C'est
 * délibérément plus strict que « tout le monde peut tout changer » : une équipe est un objet
 * partagé, et cinq personnes qui modifient le même roster sans règle donnent un roster que
 * personne ne reconnaît. Le créateur est le capitaine ; {@code OWNER} passe par son rôle, qui
 * porte {@code TEAM_EDIT} partout — il n'a donc pas besoin d'être traité ici.</p>
 *
 * <p><strong>Ce qui n'est pas accordé, et pourquoi</strong> : être membre ne donne pas
 * {@code TEAM_CREATE}. Cette permission est globale et sans ressource ; l'y ajouter reviendrait
 * à faire répondre oui à une question qui ne porte sur aucune équipe.</p>
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
