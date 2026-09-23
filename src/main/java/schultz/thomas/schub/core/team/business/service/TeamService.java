package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private static final int NOM_MAX = 60;

    private final TeamRepository teamRepository;
    private final CompositionRepository compositionRepository;
    private final TeamChampionPoolRepository championPoolRepository;
    private final GameReviewRepository reviewRepository;
    private final PermissionEvaluator permissionEvaluator;
    private final MemberDirectory memberDirectory;
    private final RiotIdResolver riotIdResolver;

    public static ResourceRef ref(String teamId) {
        return teamId == null ? null : new ResourceRef(ResourceType.TEAM, teamId);
    }

    public Team require(String teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Aucune équipe d'identifiant '" + teamId + "'"));
    }

    public Team requireVisible(User actor, String teamId) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_VIEW, ref(teamId));
        return team;
    }

    public List<Team> mine(User actor) {
        if (actor == null || actor.getId() == null) {
            return List.of();
        }
        Map<String, Team> byId = new LinkedHashMap<>();
        teamRepository.findByMembersUserId(actor.getId()).forEach(team -> byId.put(team.getId(), team));
        teamRepository.findByCreatedBy(actor.getId()).forEach(team -> byId.put(team.getId(), team));
        return byId.values().stream()
                .sorted(Comparator.comparing(Team::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public Team create(User actor, String name) {
        permissionEvaluator.require(actor, Permission.TEAM_CREATE, null);

        Team team = new Team();
        team.setName(nomValide(name));
        team.setCreatedBy(actor.getId());
        team.setMembers(new ArrayList<>());
        team.setCreatedAt(Instant.now());
        team.setUpdatedAt(team.getCreatedAt());

        Team created = teamRepository.save(team);
        log.info("Équipe « {} » créée par {}", created.getName(), actor.getDiscordId());
        return created;
    }

    public Team rename(User actor, String teamId, String name) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));
        team.setName(nomValide(name));
        return touch(team);
    }

    public void delete(User actor, String teamId) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));
        compositionRepository.deleteByTeamId(teamId);
        championPoolRepository.deleteById(teamId);
        reviewRepository.deleteByTeamId(teamId);
        teamRepository.delete(team);
        log.info("Équipe « {} » supprimée par {}", team.getName(), actor.getDiscordId());
    }

    public Team addMember(User actor, String teamId, NewMember demande) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));

        String gameName = trimOrNull(demande.riotGameName());
        String tagLine = trimOrNull(demande.riotTagLine());
        if (gameName == null || tagLine == null) {
            throw new IllegalArgumentException(
                    "Un membre est identifié par son Riot ID complet : Pseudo et TAG sont obligatoires");
        }

        String puuid = trimOrNull(demande.riotPuuid());
        if (puuid == null) {
            var resolution = riotIdResolver.resolve(gameName, tagLine);
            if (resolution.isNotFound()) {
                throw new IllegalArgumentException(
                        "Aucun compte Riot ne porte « " + gameName + "#" + tagLine + " »");
            }
            puuid = resolution.puuid();
        }

        refuseLesDoublons(team, gameName, tagLine, puuid);

        TeamMember member = new TeamMember();
        member.setMemberId(UUID.randomUUID().toString());
        member.setRiotGameName(gameName);
        member.setRiotTagLine(tagLine);
        member.setRiotPuuid(puuid);
        member.setRoles(postesValides(demande.roles()));
        member.setStatus(demande.status() == null ? MemberStatus.TITULAIRE : demande.status());
        member.setAddedAt(Instant.now());

        if (puuid != null) {
            memberDirectory.byRiotPuuid(puuid).ifPresent(identity -> {
                member.setUserId(identity.userId());
                member.setLinkedAt(Instant.now());
            });
        }
        refuseCoachAvecPoste(member);
        if (member.getUserId() != null && team.hasMemberLinkedTo(member.getUserId())) {
            throw new IllegalStateException("Ce compte est déjà dans l'effectif de cette équipe");
        }

        team.getMembers().add(member);
        log.info("Membre {} ajouté à l'équipe « {} » ({})",
                member.riotId(), team.getName(), member.isLinked() ? "lié" : "libre");
        return touch(team);
    }

    public Team updateMember(User actor, String teamId, String memberId, List<GameRole> roles,
                             MemberStatus status) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));

        TeamMember member = team.findMember(memberId)
                .orElseThrow(() -> new NoSuchElementException("Aucun membre d'identifiant '" + memberId + "'"));
        member.setRoles(postesValides(roles));
        if (status != null) {
            member.setStatus(status);
        }
        refuseCoachAvecPoste(member);
        return touch(team);
    }

    public Team removeMember(User actor, String teamId, String memberId) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));

        TeamMember member = team.findMember(memberId)
                .orElseThrow(() -> new NoSuchElementException("Aucun membre d'identifiant '" + memberId + "'"));
        team.getMembers().remove(member);
        reviewRepository.deleteByTeamIdAndSubjectMemberId(teamId, memberId);
        log.info("Membre {} retiré de l'équipe « {} »", member.riotId(), team.getName());
        return touch(team);
    }

    public List<Team> claim(User actor) {
        if (actor == null) {
            return List.of();
        }
        String puuid = trimOrNull(actor.getRiotPuuid());
        String riotId = riotIdDe(actor.getRiotGameName(), actor.getRiotTagLine());
        if (puuid == null && riotId == null) {
            throw new IllegalStateException(
                    "Aucun compte Riot lié : il n'y a rien à revendiquer tant que le Riot ID n'est pas renseigné");
        }

        List<Team> liees = new ArrayList<>();
        for (Team team : teamRepository.findAll()) {
            boolean modifiee = false;
            for (TeamMember member : team.getMembers()) {
                if (actor.getId().equals(member.getUserId())) {
                    modifiee |= resynchronise(member, actor, puuid, team.getName());
                    continue;
                }
                if (member.isLinked() || !correspond(member, puuid, riotId)) {
                    continue;
                }
                member.setUserId(actor.getId());
                member.setLinkedAt(Instant.now());
                if (member.getRiotPuuid() == null) {
                    member.setRiotPuuid(puuid);
                }
                modifiee = true;
                log.info("Membre {} de l'équipe « {} » revendiqué par {}",
                        member.riotId(), team.getName(), actor.getDiscordId());
            }
            if (modifiee) {
                liees.add(touch(team));
            }
        }
        return liees;
    }


    private boolean resynchronise(TeamMember member, User actor, String puuid, String teamName) {
        String gameName = trimOrNull(actor.getRiotGameName());
        String tagLine = trimOrNull(actor.getRiotTagLine());
        boolean puuidAJour = puuid == null || puuid.equals(member.getRiotPuuid());
        if (puuidAJour
                && Objects.equals(gameName, member.getRiotGameName())
                && Objects.equals(tagLine, member.getRiotTagLine())) {
            return false;
        }
        log.info("Place de {} dans l'équipe « {} » resynchronisée sur son compte Riot courant : {} devient {}",
                actor.getDiscordId(), teamName, member.riotId(), riotIdDe(gameName, tagLine));
        if (puuid != null) {
            member.setRiotPuuid(puuid);
        }
        member.setRiotGameName(gameName);
        member.setRiotTagLine(tagLine);
        return true;
    }

    private boolean correspond(TeamMember member, String puuid, String riotId) {
        if (puuid != null && puuid.equals(member.getRiotPuuid())) {
            return true;
        }
        return member.getRiotPuuid() == null
                && riotId != null
                && riotId.equalsIgnoreCase(member.riotId());
    }

    private void refuseLesDoublons(Team team, String gameName, String tagLine, String puuid) {
        String riotId = riotIdDe(gameName, tagLine);
        boolean deja = team.getMembers().stream().anyMatch(member ->
                (puuid != null && puuid.equals(member.getRiotPuuid()))
                        || (riotId != null && riotId.equalsIgnoreCase(member.riotId())));
        if (deja) {
            throw new IllegalStateException("Ce joueur est déjà dans l'effectif de cette équipe");
        }
    }

    private void refuseCoachAvecPoste(TeamMember member) {
        if (member.getStatus() == MemberStatus.COACH && !member.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Un coach ne tient pas de poste : laisser la liste vide");
        }
    }

    private static List<GameRole> postesValides(List<GameRole> roles) {
        if (roles == null) {
            return new ArrayList<>();
        }
        return roles.stream().filter(Objects::nonNull).distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String nomValide(String name) {
        String propre = trimOrNull(name);
        if (propre == null) {
            throw new IllegalArgumentException("Une équipe a un nom");
        }
        if (propre.length() > NOM_MAX) {
            throw new IllegalArgumentException("Le nom d'une équipe ne dépasse pas " + NOM_MAX + " caractères");
        }
        return propre;
    }

    private Team touch(Team team) {
        team.setUpdatedAt(Instant.now());
        return teamRepository.save(team);
    }

    private static String riotIdDe(String gameName, String tagLine) {
        String g = trimOrNull(gameName);
        String t = trimOrNull(tagLine);
        return g == null || t == null ? null : g + "#" + t;
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String propre = value.trim();
        return propre.isEmpty() ? null : propre;
    }

    public record NewMember(
            String riotGameName,
            String riotTagLine,
            String riotPuuid,
            List<GameRole> roles,
            MemberStatus status
    ) {
    }

    public Optional<TeamMember> jouable(Team team, String memberId) {
        return team.findMember(memberId).filter(member -> member.getStatus() != MemberStatus.COACH);
    }
}
