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

@Slf4j
@Service
@RequiredArgsConstructor
public class CompositionService {

    private static final int NOM_MAX = 60;
    private static final int BANS_MAX = 5;
    private static final int REMPLACANTS_MAX = 5;

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
        List<String> bans = bansValides(draft.bans());
        composition.setSlots(valide(team, draft.slots(), bans));
        composition.setBans(bans);
        composition.setPatch(trimOrNull(draft.patch()));
        composition.setNotes(trimOrNull(draft.notes()));
        composition.setUpdatedAt(Instant.now());
    }

    private List<String> bansValides(List<String> bans) {
        List<String> propres = champions(bans, "banni");
        if (propres.size() > BANS_MAX) {
            throw new IllegalArgumentException("Une équipe bannit " + BANS_MAX + " champions au plus, pas "
                    + propres.size());
        }
        return propres;
    }

    List<CompositionSlot> valide(Team team, List<CompositionSlot> slots, List<String> bans) {
        if (slots == null || slots.size() != GameRole.values().length) {
            throw new IllegalArgumentException("Une composition désigne exactement "
                    + GameRole.values().length + " postes, pas " + (slots == null ? 0 : slots.size()));
        }

        Set<GameRole> postes = EnumSet.noneOf(GameRole.class);
        Set<String> joueurs = new HashSet<>();
        Set<String> champions = new HashSet<>();
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
            if (bans.contains(champion)) {
                throw new IllegalArgumentException(champion + " est banni : il ne peut pas tenir le poste "
                        + slot.getRole());
            }
            if (!champions.add(champion)) {
                throw new IllegalArgumentException(champion + " est choisi à deux postes");
            }
            List<String> alternatives = alternativesValides(slot, champion, bans);
            String memberId = trimOrNull(slot.getMemberId());
            if (memberId != null) {
                teamService.jouable(team, memberId).orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur désigné au poste " + slot.getRole()
                                + " n'est pas un joueur de cette équipe"));
                if (!joueurs.add(memberId)) {
                    throw new IllegalArgumentException("Un même joueur est désigné à deux postes");
                }
            }
            propres.add(new CompositionSlot(slot.getRole(), champion, memberId, alternatives));
        }
        return propres;
    }

    private List<String> alternativesValides(CompositionSlot slot, String champion, List<String> bans) {
        List<String> alternatives = champions(slot.getAlternatives(), "remplaçant");
        if (alternatives.size() > REMPLACANTS_MAX) {
            throw new IllegalArgumentException("Le poste " + slot.getRole() + " propose " + REMPLACANTS_MAX
                    + " remplaçants au plus");
        }
        if (alternatives.contains(champion)) {
            throw new IllegalArgumentException(champion + " ne remplace pas lui-même au poste " + slot.getRole());
        }
        alternatives.stream().filter(bans::contains).findFirst().ifPresent(banni -> {
            throw new IllegalArgumentException(banni + " est banni : il ne peut pas remplacer au poste "
                    + slot.getRole());
        });
        return alternatives;
    }

    private static List<String> champions(List<String> saisis, String nature) {
        if (saisis == null) {
            return new ArrayList<>();
        }
        List<String> propres = new ArrayList<>();
        for (String saisi : saisis) {
            String champion = trimOrNull(saisi);
            if (champion == null) {
                throw new IllegalArgumentException("Un champion " + nature + " est vide");
            }
            if (propres.contains(champion)) {
                throw new IllegalArgumentException(champion + " est " + nature + " deux fois");
            }
            propres.add(champion);
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

    public record Draft(String name, List<CompositionSlot> slots, List<String> bans, String patch, String notes) {
    }
}
