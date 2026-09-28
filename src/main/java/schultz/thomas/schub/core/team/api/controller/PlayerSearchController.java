package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.ActivityService;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotConnectorService.HistoryWindow;
import schultz.thomas.schub.core.team.api.dto.PlayerSuggestionDto;

import java.util.List;

// Public : les comptes déjà connus seulement, sans appel à Riot. Un Riot ID complet se résout sur sa page.
@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerSearchController {

    private static final int MIN_QUERY = 3;
    private static final int MAX_LIMIT = 10;

    private final RiotConnectorService riotConnector;
    private final ActivityService activityService;

    // Pour le plan du site : des Riot ID, jamais de puuid.
    @GetMapping("/tracked")
    public List<String> tracked(@RequestParam(defaultValue = "50000") int limit) {
        return riotConnector.trackedRiotIds(Math.clamp(limit, 1, 50_000));
    }

    // La présentation publique dit ce que couvre l'historique d'un joueur.
    @GetMapping("/history-window")
    public ResponseEntity<HistoryWindow> historyWindow() {
        return riotConnector.historyWindow()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/search")
    public List<PlayerSuggestionDto> search(@RequestParam String q,
                                            @RequestParam(defaultValue = "8") int limit,
                                            @RequestHeader(value = CoreHeaders.VISITOR, required = false) String visitor) {
        String pseudo = q.split("#")[0].trim();
        if (pseudo.length() < MIN_QUERY) {
            return List.of();
        }
        activityService.recordSearch(visitor);
        return riotConnector.search(pseudo, Math.clamp(limit, 1, MAX_LIMIT)).stream()
                .filter(joueur -> joueur.gameName() != null && joueur.tagLine() != null)
                .map(joueur -> new PlayerSuggestionDto(joueur.riotId(), joueur.gameName(), joueur.tagLine(),
                        joueur.matchCount(),
                        joueur.positions().stream().map(RiotConnectorService.PositionPlayed::position).toList(),
                        joueur.lastPlayedAt()))
                .toList();
    }
}
