package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.business.service.MyStatsService;

/**
 * {@code GET /me/stats}, et jamais {@code /players/{puuid}/stats}.
 *
 * <p>Même règle que le reste de {@code /me} : aucun second paramètre, donc aucune cible possible.
 * Un chemin portant un puuid laisserait n'importe qui sonder l'historique de n'importe qui à
 * partir d'un puuid croisé dans une réponse d'équipe.</p>
 *
 * <p>Le contrôleur est dans le paquet {@code team} avec le reste du domaine LoL, là où la route
 * prolonge {@code /me} : le découpage suit le domaine, l'URL suit le sujet.</p>
 */
@RestController
@RequestMapping("/me/stats")
@RequiredArgsConstructor
public class MyStatsController {

    private final MyStatsService myStatsService;
    private final UserService userService;

    @GetMapping
    public MyStatsDto mine(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer champions) {
        return myStatsService.of(userService.requireActor(actorDiscordId), days, champions);
    }
}
