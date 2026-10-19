package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.team.api.dto.ChampionGridDto;
import schultz.thomas.schub.core.team.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.team.api.mapper.LolDtoMapper;
import schultz.thomas.schub.core.team.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.team.data.model.GameRole;

import java.util.Set;

// Public en lecture : agrégats par palier et par champion, sans donnée personnelle (page publique d'un joueur).
// 204 : pas encore de référentiel pour ce poste (population trop mince, ou connecteur muet). L'écran note sans icône.
@RestController
@RequestMapping("/lol/references")
@RequiredArgsConstructor
public class ReferenceController {

    private static final Set<String> PORTEES = Set.of("GAME", "MEAN");

    private final RiotStatsGateway statsGateway;
    private final LolDtoMapper dtos;

    @GetMapping("/champions/{championId}")
    public ResponseEntity<ChampionGridDto> champion(@PathVariable int championId, @RequestParam String tier) {
        return statsGateway.championGrid(championId, tier)
                .map(dtos::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{position}")
    public ResponseEntity<ReferenceGridDto> grid(
            @PathVariable String position,
            @RequestParam(defaultValue = "MEAN") String scope,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String patch) {
        if (!GameRole.estUnPoste(position) || !PORTEES.contains(scope)) {
            return ResponseEntity.badRequest().build();
        }
        return statsGateway.referenceGrid(position, scope, tier, patch)
                .map(dtos::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
