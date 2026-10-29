package schultz.thomas.schub.core.lol.team.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.team.data.model.MacroPlan;
import schultz.thomas.schub.core.lol.team.data.repository.MacroPlanRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

// Les plans du Macro Planner (Schub#172). Tout membre qui voit l'équipe les lit et les modifie.
@Slf4j
@Service
@RequiredArgsConstructor
public class MacroPlanService {

    private static final int NOM_MAX = 60;
    private static final int ELEMENTS_MAX = 5_000;
    private static final int OCTETS_MAX = 1_000_000;

    private final MacroPlanRepository repository;
    private final TeamService teamService;
    private final ObjectMapper json;

    public List<MacroPlan> ofTeam(User actor, String teamId) {
        teamService.requireVisible(actor, teamId);
        return repository.findByTeamIdOrderByUpdatedAtDesc(teamId);
    }

    public MacroPlan require(User actor, String teamId, String planId) {
        teamService.requireVisible(actor, teamId);
        return repository.findById(planId)
                .filter(plan -> teamId.equals(plan.getTeamId()))
                .orElseThrow(() -> new NoSuchElementException("Aucun plan d'identifiant '" + planId + "' dans cette équipe"));
    }

    public MacroPlan create(User actor, String teamId, String name, List<Map<String, Object>> elements) {
        teamService.requireVisible(actor, teamId);
        MacroPlan plan = new MacroPlan();
        plan.setTeamId(teamId);
        plan.setName(nomValide(name));
        plan.setElements(elementsValides(elements == null ? List.of() : elements));
        plan.setCreatedBy(actor.getId());
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedBy(actor.getId());
        plan.setUpdatedAt(plan.getCreatedAt());
        MacroPlan cree = repository.save(plan);
        log.info("Plan de macro « {} » créé pour l'équipe {} par {}", cree.getName(), teamId, actor.getId());
        return cree;
    }

    // name ou elements absents : inchangés. Le front enregistre l'un sans l'autre.
    public MacroPlan update(User actor, String teamId, String planId, String name, List<Map<String, Object>> elements) {
        MacroPlan plan = require(actor, teamId, planId);
        if (name != null) {
            plan.setName(nomValide(name));
        }
        if (elements != null) {
            plan.setElements(elementsValides(elements));
        }
        plan.setUpdatedBy(actor.getId());
        plan.setUpdatedAt(Instant.now());
        return repository.save(plan);
    }

    public void delete(User actor, String teamId, String planId) {
        repository.delete(require(actor, teamId, planId));
    }

    private List<Map<String, Object>> elementsValides(List<Map<String, Object>> elements) {
        if (elements.size() > ELEMENTS_MAX) {
            throw new IllegalArgumentException("Un plan compte " + ELEMENTS_MAX + " éléments au plus");
        }
        if (elements.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Un élément du plan est vide");
        }
        try {
            if (json.writeValueAsBytes(elements).length > OCTETS_MAX) {
                throw new IllegalArgumentException("Un plan pèse " + OCTETS_MAX / 1000 + " Ko au plus");
            }
        } catch (JsonProcessingException illisible) {
            throw new IllegalArgumentException("Les éléments du plan ne se lisent pas en JSON");
        }
        return new ArrayList<>(elements);
    }

    private static String nomValide(String name) {
        String propre = name == null ? "" : name.strip();
        if (propre.isEmpty()) {
            throw new IllegalArgumentException("Un plan a un nom");
        }
        if (propre.length() > NOM_MAX) {
            throw new IllegalArgumentException("Le nom d'un plan ne dépasse pas " + NOM_MAX + " caractères");
        }
        return propre;
    }
}
