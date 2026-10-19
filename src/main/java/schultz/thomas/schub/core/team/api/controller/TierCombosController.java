package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.team.api.dto.TierCombosDto;
import schultz.thomas.schub.core.team.business.service.TierCombosService;

// Public : aucune équipe ni aucun joueur, seulement des champions comptés sur l'échantillon.
@RestController
@RequestMapping("/tier-combos")
@RequiredArgsConstructor
public class TierCombosController {

    private final TierCombosService tierCombos;

    @GetMapping("/{tier}/{kind}")
    public TierCombosDto of(@PathVariable String tier, @PathVariable String kind) {
        return tierCombos.of(tier, kind);
    }
}
