package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.CompositionDto;
import schultz.thomas.schub.core.team.api.dto.CompositionSlotDto;
import schultz.thomas.schub.core.team.api.dto.TeamDto;
import schultz.thomas.schub.core.team.api.dto.TeamMemberDto;
import schultz.thomas.schub.core.team.api.dto.TeamSummaryDto;
import schultz.thomas.schub.core.team.data.model.Composition;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Traduit une équipe pour son lecteur — et calcule ce qu'il a le droit d'y faire.
 *
 * <h2>Pourquoi des booléens et pas la liste des ayants droit</h2>
 *
 * <p>Le front doit savoir s'il peut modifier une équipe. La réponse évidente — lui donner le
 * créateur et le laisser comparer — a déjà été jugée et écartée au §A.5 bis du plan : c'est une
 * information sur les <em>autres</em>, elle oblige le client à rejouer une règle d'autorisation,
 * et elle répond faux dès qu'un {@code OWNER} regarde. La forme retenue est un fait sur le
 * lecteur, exactement comme {@code viewerIsAdmin} sur un serveur.</p>
 *
 * <p><strong>Une seule évaluation par équipe.</strong> Les permissions effectives sont demandées
 * une fois et les trois faits en sont dérivés : trois appels séparés feraient trois lectures de
 * la même équipe, et sur une liste, trois par ligne.</p>
 */
@Service
@RequiredArgsConstructor
public class TeamProjectionService {

    private final PermissionEvaluator permissionEvaluator;
    private final MemberDirectory memberDirectory;

    public List<TeamSummaryDto> toSummaries(List<Team> teams, User actor) {
        return teams.stream().map(team -> toSummary(team, actor)).toList();
    }

    public TeamSummaryDto toSummary(Team team, User actor) {
        Set<Permission> droits = droitsSur(team, actor);
        return new TeamSummaryDto(
                team.getId(),
                team.getName(),
                team.getMembers() == null ? 0 : team.getMembers().size(),
                team.getCreatedAt(),
                team.getUpdatedAt(),
                placeDuLecteur(team, actor).orElse(null),
                droits.contains(Permission.TEAM_EDIT),
                droits.contains(Permission.COMPOSITION_EDIT));
    }

    public TeamDto toDto(Team team, User actor) {
        Set<Permission> droits = droitsSur(team, actor);
        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(team);
        List<TeamMemberDto> membres = team.getMembers() == null ? List.of()
                : team.getMembers().stream().map(member -> toDto(member, team, identites)).toList();

        return new TeamDto(
                team.getId(),
                team.getName(),
                membres.size(),
                membres,
                team.getCreatedAt(),
                team.getUpdatedAt(),
                placeDuLecteur(team, actor).orElse(null),
                droits.contains(Permission.TEAM_EDIT),
                droits.contains(Permission.COMPOSITION_EDIT));
    }

    public List<CompositionDto> toDtos(List<Composition> compositions, Team team, User actor) {
        boolean peutEcrire = droitsSur(team, actor).contains(Permission.COMPOSITION_EDIT);
        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(team);
        return compositions.stream().map(composition -> toDto(composition, team, identites, peutEcrire)).toList();
    }

    public CompositionDto toDto(Composition composition, Team team, User actor) {
        return toDto(composition, team, resoutLesComptes(team),
                droitsSur(team, actor).contains(Permission.COMPOSITION_EDIT));
    }

    // --- interne ---

    private CompositionDto toDto(Composition composition, Team team,
                                 Map<String, MemberDirectory.MemberIdentity> identites, boolean peutEcrire) {
        List<CompositionSlotDto> slots = composition.getSlots() == null ? List.of()
                : composition.getSlots().stream()
                .map(slot -> new CompositionSlotDto(
                        slot.getRole(),
                        slot.getChampionId(),
                        slot.getMemberId(),
                        team.findMember(slot.getMemberId())
                                .map(member -> nomAffiche(member, identites))
                                .orElse(null)))
                .toList();
        return new CompositionDto(
                composition.getId(),
                composition.getTeamId(),
                composition.getName(),
                slots,
                composition.getPatch(),
                composition.getNotes(),
                composition.getCreatedAt(),
                composition.getUpdatedAt(),
                peutEcrire);
    }

    private TeamMemberDto toDto(TeamMember member, Team team,
                                Map<String, MemberDirectory.MemberIdentity> identites) {
        MemberDirectory.MemberIdentity identite =
                member.getUserId() == null ? null : identites.get(member.getUserId());
        return new TeamMemberDto(
                member.getMemberId(),
                nomAffiche(member, identites),
                identite == null ? null : identite.avatarUrl(),
                member.getRiotGameName(),
                member.getRiotTagLine(),
                member.getRole(),
                member.getStatus(),
                member.isLinked(),
                member.getUserId() != null && member.getUserId().equals(team.getCreatedBy()));
    }

    /**
     * Le pseudo du compte quand le membre est lié, son Riot ID sinon.
     *
     * <p>Résolu à la lecture et jamais recopié dans l'équipe : un pseudo recopié est faux le jour
     * où il change, et personne ne s'en aperçoit.</p>
     */
    private String nomAffiche(TeamMember member, Map<String, MemberDirectory.MemberIdentity> identites) {
        if (member.getUserId() != null) {
            MemberDirectory.MemberIdentity identite = identites.get(member.getUserId());
            if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
                return identite.displayName();
            }
        }
        return member.riotId();
    }

    /** Un seul aller-retour vers l'identité pour toute l'équipe, jamais un par membre. */
    private Map<String, MemberDirectory.MemberIdentity> resoutLesComptes(Team team) {
        if (team.getMembers() == null || team.getMembers().isEmpty()) {
            return Map.of();
        }
        Set<String> ids = new HashSet<>();
        team.getMembers().stream()
                .map(TeamMember::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        return ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
    }

    private Optional<String> placeDuLecteur(Team team, User actor) {
        if (actor == null || actor.getId() == null || team.getMembers() == null) {
            return Optional.empty();
        }
        return team.getMembers().stream()
                .filter(member -> actor.getId().equals(member.getUserId()))
                .map(TeamMember::getMemberId)
                .findFirst();
    }

    private Set<Permission> droitsSur(Team team, User actor) {
        return permissionEvaluator.effectivePermissions(actor, TeamService.ref(team.getId()));
    }
}
