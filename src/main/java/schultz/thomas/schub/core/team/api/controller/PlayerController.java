package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.PlayerCollectDto;
import schultz.thomas.schub.core.team.api.dto.SearchedPlayerDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.service.SearchedPlayerService;
import schultz.thomas.schub.core.team.business.service.StatsWindows;

// Public : un joueur se désigne par son Riot ID (Nom-TAG), jamais par un puuid.
@RestController
@RequestMapping("/players/{riotId}")
@RequiredArgsConstructor
public class PlayerController {

    public static final String VISITOR_ID = "X-Visitor-Id";

    private final SearchedPlayerService players;
    private final StatsWindows windows;

    @GetMapping
    public SearchedPlayerDto page(@PathVariable String riotId,
                                  @RequestParam(required = false) Integer days,
                                  @RequestParam(required = false) Integer patches,
                                  @RequestParam(required = false) Integer champions,
                                  @RequestParam(defaultValue = "false") boolean light) {
        return players.page(riotId, windows.days(days, patches), champions, light);
    }

    @GetMapping("/games")
    public MyGamesDto games(@PathVariable String riotId,
                            @RequestParam(required = false) Integer days,
                            @RequestParam(required = false) Integer patches,
                            @RequestParam(required = false) Integer limit) {
        return players.games(riotId, windows.days(days, patches), limit);
    }

    @GetMapping("/games/{matchId}")
    public TeamGameDetailDto game(@PathVariable String riotId,
                                  @PathVariable String matchId,
                                  @RequestParam(required = false) Integer days,
                                  @RequestParam(required = false) Integer patches) {
        return players.game(riotId, matchId, windows.days(days, patches));
    }

    @PostMapping("/collect")
    public PlayerCollectDto collect(@PathVariable String riotId,
                                    @RequestHeader(value = VISITOR_ID, required = false) String visitor) {
        return players.collect(riotId, visitor);
    }
}
