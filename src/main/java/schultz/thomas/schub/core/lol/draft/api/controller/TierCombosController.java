package schultz.thomas.schub.core.lol.draft.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.lol.draft.api.dto.TierCombosDto;
import schultz.thomas.schub.core.lol.draft.api.mapper.DraftDtoMapper;
import schultz.thomas.schub.core.lol.draft.business.service.TierCombosService;

// Public : aucune équipe ni aucun joueur, seulement des champions comptés sur l'échantillon.
@RestController
@RequestMapping("/tier-combos")
@RequiredArgsConstructor
public class TierCombosController {

    private final TierCombosService tierCombos;
    private final DraftDtoMapper dtos;

    @GetMapping("/{tier}/{kind}")
    public TierCombosDto of(@PathVariable String tier, @PathVariable String kind) {
        return dtos.toDto(tierCombos.of(tier, kind));
    }
}
