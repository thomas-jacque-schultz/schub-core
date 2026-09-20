package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.data.model.Composition;
import schultz.thomas.schub.core.team.data.model.CompositionSlot;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Les compositions préparées d'une équipe.
 *
 * <p>Tout l'intérêt de cette classe est dans {@link #valide(Team, List)} : une composition qui
 * ne désigne pas exactement cinq postes n'est pas une composition, et l'accepter en base
 * donnerait un écran qui plante ou, pire, qui affiche quatre colonnes sans le dire.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompositionService {

    private static final int NOM_MAX = 60;

    private final CompositionRepository compositionRepository;
    private final TeamService teamService;
    private final PermissionEvaluator permissionEvaluator;

    public List<Composition> ofTeam(User actor, String teamId) {
        teamService.requireVisible(actor, teamId);
        return compositionRepository.findByTeamIdOrderByUpdatedAtDesc(teamId);
    }

    public Composition require(User actor, String teamId, String compositionId) {
        teamService.requireVisible(actor, teamId);
        Composition composition = compositionRepository.findById(compositionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Aucune composition d'identifiant '" + compositionId + "'"));
        if (!teamId.equals(composition.getTeamId())) {
            throw new NoSuchElementException(
                    "La composition '" + compositionId + "' n'appartient pas à cette équipe");
        }
        return composition;
    }

    public Composition create(User actor, String teamId, Draft draft) {
        Team team = teamService.require(teamId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));

        Composition composition = new Composition();
        composition.setTeamId(teamId);
        composition.setCreatedBy(actor.getId());
        composition.setCreatedAt(Instant.now());
        applique(composition, team, draft);

        Composition created = compositionRepository.save(composition);
        log.info("Composition « {} » créée pour l'équipe « {} » par {}",
                created.getName(), team.getName(), actor.getDiscordId());
        return created;
    }

    public Composition update(User actor, String teamId, String compositionId, Draft draft) {
        Team team = teamService.require(teamId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));

        Composition composition = compositionRepository.findById(compositionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Aucune composition d'identifiant '" + compositionId + "'"));
        if (!teamId.equals(composition.getTeamId())) {
            throw new NoSuchElementException(
                    "La composition '" + compositionId + "' n'appartient pas à cette équipe");
        }
        applique(composition, team, draft);
        return compositionRepository.save(composition);
    }

    public void delete(User actor, String teamId, String compositionId) {
        Composition composition = require(actor, teamId, compositionId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));
        compositionRepository.delete(composition);
    }

    private void applique(Composition composition, Team team, Draft draft) {
        composition.setName(nomValide(draft.name()));
        composition.setSlots(valide(team, draft.slots()));
        composition.setPatch(trimOrNull(draft.patch()));
        composition.setNotes(trimOrNull(draft.notes()));
        composition.setUpdatedAt(Instant.now());
    }

    /**
     * <strong>Exactement cinq lignes, un poste chacune.</strong>
     *
     * <p>Quatre postes, ou deux fois le même, ce n'est pas une équipe sur la Faille — et le
     * refuser ici plutôt qu'à l'affichage est ce qui évite qu'une composition fausse dorme en
     * base jusqu'au jour où quelqu'un la rouvre avant une partie.</p>
     *
     * <p>Le joueur, lui, reste facultatif : une composition est un <em>brouillon</em> qu'on
     * prépare avant que l'effectif soit complet (plan §D, lot D.6). Ce qui est exigé, c'est cinq
     * postes ; ce qui est vérifié quand un joueur est nommé, c'est qu'il est bien de l'équipe,
     * qu'il n'y figure pas deux fois, et qu'il n'est pas le coach.</p>
     */
    List<CompositionSlot> valide(Team team, List<CompositionSlot> slots) {
        if (slots == null || slots.size() != GameRole.values().length) {
            throw new IllegalArgumentException("Une composition désigne exactement "
                    + GameRole.values().length + " postes, pas " + (slots == null ? 0 : slots.size()));
        }

        Set<GameRole> postes = EnumSet.noneOf(GameRole.class);
        Set<String> joueurs = new HashSet<>();
        List<CompositionSlot> propres = new ArrayList<>();

        for (CompositionSlot slot : slots) {
            if (slot == null || slot.getRole() == null) {
                throw new IllegalArgumentException("Chaque ligne d'une composition porte un poste");
            }
            if (!postes.add(slot.getRole())) {
                throw new IllegalArgumentException("Le poste " + slot.getRole() + " est désigné deux fois");
            }
            String champion = trimOrNull(slot.getChampionId());
            if (champion == null) {
                throw new IllegalArgumentException("Le poste " + slot.getRole() + " est sans champion");
            }
            String memberId = trimOrNull(slot.getMemberId());
            if (memberId != null) {
                teamService.jouable(team, memberId).orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur désigné au poste " + slot.getRole()
                                + " n'est pas un joueur de cette équipe"));
                if (!joueurs.add(memberId)) {
                    throw new IllegalArgumentException("Un même joueur est désigné à deux postes");
                }
            }
            propres.add(new CompositionSlot(slot.getRole(), champion, memberId));
        }
        return propres;
    }

    private String nomValide(String name) {
        String propre = trimOrNull(name);
        if (propre == null) {
            throw new IllegalArgumentException("Une composition a un nom");
        }
        if (propre.length() > NOM_MAX) {
            throw new IllegalArgumentException("Le nom d'une composition ne dépasse pas " + NOM_MAX + " caractères");
        }
        return propre;
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String propre = value.trim();
        return propre.isEmpty() ? null : propre;
    }

    /** Ce qu'on enregistre : un nom, cinq lignes, le patch qui leur donne leur sens, des notes. */
    public record Draft(String name, List<CompositionSlot> slots, String patch, String notes) {
    }
}
