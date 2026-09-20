package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Les équipes : leur création, leur effectif, et la revendication d'un membre libre.
 *
 * <p>Toutes les écritures passent par {@link PermissionEvaluator} <em>au point d'action</em>,
 * avec l'équipe comme ressource. Le contrôle grossier du BFF ne peut pas répondre à « est-ce
 * que celui-là est le capitaine de celle-ci ? » — seul le cœur le sait (plan §A.2).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private static final int NOM_MAX = 60;

    private final TeamRepository teamRepository;
    private final CompositionRepository compositionRepository;
    private final PermissionEvaluator permissionEvaluator;
    private final MemberDirectory memberDirectory;
    private final RiotIdResolver riotIdResolver;

    /** La référence de portée d'une équipe. Son <em>id</em> : une équipe n'a pas de slug. */
    public static ResourceRef ref(String teamId) {
        return teamId == null ? null : new ResourceRef(ResourceType.TEAM, teamId);
    }

    // --- lecture ---

    public Team require(String teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Aucune équipe d'identifiant '" + teamId + "'"));
    }

    /** Lit une équipe, ou refuse. En être membre suffit ; ne pas l'être ne suffit pas. */
    public Team requireVisible(User actor, String teamId) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_VIEW, ref(teamId));
        return team;
    }

    /**
     * Les équipes de cet acteur — <strong>c'est ce qui les fait « apparaître sur son compte »</strong>.
     *
     * <p>Deux sources, dédoublonnées : celles où il figure dans l'effectif, et celles qu'il a
     * créées. La seconde n'est pas redondante — on peut créer une équipe sans s'y mettre soi-même,
     * et elle disparaîtrait de la vue de son propre capitaine.</p>
     *
     * <p>Un compte qui porte {@code TEAM_VIEW} par son rôle (aujourd'hui {@code ADMINISTRATOR} et
     * {@code OWNER}) ne reçoit pas pour autant toutes les équipes du système ici : cette route
     * répond « les miennes », pas « celles que j'ai le droit de voir ». Mélanger les deux
     * donnerait à un administrateur une liste qui n'est pas la sienne, sans qu'il l'ait demandé.</p>
     */
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

    // --- écriture ---

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

    /**
     * Supprime l'équipe <strong>et ses compositions</strong>.
     *
     * <p>Les laisser derrière donnerait des documents qui référencent une équipe disparue :
     * invisibles, jamais relus, et découverts un jour par une requête qui compte mal.</p>
     */
    public void delete(User actor, String teamId) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));
        compositionRepository.deleteByTeamId(teamId);
        teamRepository.delete(team);
        log.info("Équipe « {} » supprimée par {}", team.getName(), actor.getDiscordId());
    }

    /**
     * Ajoute quelqu'un à l'effectif, lié à un compte Schub ou libre.
     *
     * <p>Le membre est lié <em>d'emblée</em> si son {@code puuid} désigne déjà un compte : sans
     * ça, une personne qui a un compte Schub devrait se déconnecter et se reconnecter pour voir
     * apparaître une équipe qu'on vient de créer pour elle.</p>
     */
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
            puuid = riotIdResolver.resolvePuuid(gameName, tagLine).orElse(null);
        }

        refuseLesDoublons(team, gameName, tagLine, puuid);

        TeamMember member = new TeamMember();
        member.setMemberId(UUID.randomUUID().toString());
        member.setRiotGameName(gameName);
        member.setRiotTagLine(tagLine);
        member.setRiotPuuid(puuid);
        member.setRole(demande.role());
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

    public Team updateMember(User actor, String teamId, String memberId, GameRole role, MemberStatus status) {
        Team team = require(teamId);
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, ref(teamId));

        TeamMember member = team.findMember(memberId)
                .orElseThrow(() -> new NoSuchElementException("Aucun membre d'identifiant '" + memberId + "'"));
        member.setRole(role);
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
        log.info("Membre {} retiré de l'équipe « {} »", member.riotId(), team.getName());
        return touch(team);
    }

    /**
     * <strong>La revendication</strong> : l'acteur récupère les places qui l'attendaient.
     *
     * <p>C'est le moment décrit par le plan §D.2 bis — « un membre libre devient lié le jour où
     * la personne se connecte et revendique son Riot ID », et c'est ce qui fait apparaître
     * l'équipe sur son compte. À appeler après la liaison de compte Riot (lot D.3) ; l'appeler
     * deux fois ne fait rien de plus, l'opération est idempotente.</p>
     *
     * <p>Deux façons de reconnaître une place : par {@code puuid} quand le membre en a un, sinon
     * par Riot ID écrit à la main. La seconde est nécessaire parce qu'un membre peut avoir été
     * ajouté alors que le connecteur Riot était indisponible ; elle est moins sûre — deux
     * personnes peuvent avoir écrit le même Riot ID — mais elle ne compare que des membres
     * <em>libres</em>, donc elle ne prend jamais la place de quelqu'un.</p>
     *
     * <p>La lecture est complète, et c'est assumé : une revendication arrive une fois par
     * personne, sur un volume qui se compte en dizaines d'équipes. Un index pour ça serait payer
     * un problème qui n'existe pas — et il ne couvrirait de toute façon que la moitié des
     * correspondances, celles qui ont un {@code puuid}.</p>
     *
     * <p><strong>Aucune permission n'est exigée, et c'est volontaire</strong> : on ne revendique
     * que sa propre identité Riot, celle qui est écrite sur son propre compte. Refuser cet appel
     * à un {@code VISITEUR} reviendrait à lui interdire d'entrer dans une équipe où quelqu'un
     * l'a déjà inscrit.</p>
     */
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

    // --- règles ---

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

    /**
     * Un coach n'a pas de poste. Le tolérer donnerait un TOP qui n'est pas dans le jeu, et une
     * composition qui pourrait le retenir.
     */
    private void refuseCoachAvecPoste(TeamMember member) {
        if (member.getStatus() == MemberStatus.COACH && member.getRole() != null) {
            throw new IllegalArgumentException("Un coach ne tient pas de poste : laisser le rôle vide");
        }
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

    /**
     * Ce qu'il faut pour ajouter quelqu'un. Le {@code puuid} est facultatif : fourni, il évite un
     * appel au connecteur Riot ; absent, il est résolu au mieux.
     */
    public record NewMember(
            String riotGameName,
            String riotTagLine,
            String riotPuuid,
            GameRole role,
            MemberStatus status
    ) {
    }

    /** Un membre existe-t-il sous cet identifiant, et peut-il être retenu dans une composition ? */
    public Optional<TeamMember> jouable(Team team, String memberId) {
        return team.findMember(memberId).filter(member -> member.getStatus() != MemberStatus.COACH);
    }
}
