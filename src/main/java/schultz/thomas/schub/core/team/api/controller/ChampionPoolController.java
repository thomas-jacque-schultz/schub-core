package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.business.service.ChampionPoolService;

/**
 * Le pool de champions d'une équipe — panneau 3 de sa page (plan §D.5).
 *
 * <p>Sous {@code /teams/{teamId}} pour la même raison que les compositions : le pool n'existe pas
 * sans son équipe, et c'est elle qui porte les droits. Une route à plat obligerait à retrouver
 * l'équipe pour savoir qui a le droit de lire.</p>
 *
 * <p>Protégée par {@code TEAM_VIEW} <strong>sur cette équipe</strong> — en être membre suffit, ne
 * pas l'être ne suffit pas. La vérification est faite par le service, au point d'action, avec la
 * même chaîne que partout ailleurs ; rien n'est redit ici.</p>
 */
@RestController
@RequestMapping("/teams/{teamId}/champion-pool")
@RequiredArgsConstructor
public class ChampionPoolController {

    private final ChampionPoolService championPoolService;
    private final UserService userService;

    /**
     * @param champions combien de champions par membre, du plus maîtrisé au moins maîtrisé.
     *                  Absent = 10. Une valeur hors bornes est ramenée dans les bornes, et la
     *                  valeur appliquée est rendue dans la réponse
     */
    @GetMapping
    public ChampionPoolDto byTeam(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer champions) {
        return championPoolService.of(userService.requireActor(actorDiscordId), teamId, champions);
    }
}
