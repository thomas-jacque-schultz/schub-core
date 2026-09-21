package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.PoolChampionsRequest;
import schultz.thomas.schub.core.team.api.dto.PoolMasteryFloorRequest;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.service.ChampionPoolService;

/**
 * Le pool de champions d'une équipe — panneau 3 de sa page (plan §D.5).
 *
 * <p>Sous {@code /teams/{teamId}} pour la même raison que les compositions : le pool n'existe pas
 * sans son équipe, et c'est elle qui porte les droits.</p>
 *
 * <p>Lecture sous {@code TEAM_VIEW}, écriture sous {@code COMPOSITION_EDIT}, l'une et l'autre
 * <strong>sur cette équipe</strong>. Choisir ce qu'on peut aligner est du même ordre que préparer
 * une composition — c'est la même matière et le même geste ; l'attacher à {@code TEAM_EDIT} aurait
 * rangé un travail de préparation avec la gestion de l'effectif. Le service vérifie au point
 * d'action ; rien n'est redit ici.</p>
 *
 * <p>Les deux écritures rendent le panneau entier : un seul appel suffit à redessiner l'écran.</p>
 */
@RestController
@RequestMapping("/teams/{teamId}/champion-pool")
@RequiredArgsConstructor
public class ChampionPoolController {

    private final ChampionPoolService championPoolService;
    private final UserService userService;

    /**
     * @param masteryFloor un plancher pour cette lecture seulement. Absent = celui de l'équipe.
     *                     Lire n'écrit jamais le réglage : la réponse porte les deux valeurs
     */
    @GetMapping
    public ChampionPoolDto byTeam(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer masteryFloor) {
        return championPoolService.of(userService.requireActor(actorDiscordId), teamId, masteryFloor);
    }

    @PutMapping("/roles/{role}")
    public ChampionPoolDto setChampions(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable GameRole role,
            @RequestBody PoolChampionsRequest request) {
        return championPoolService.setChampions(userService.requireActor(actorDiscordId), teamId, role,
                request == null ? null : request.championKeys());
    }

    @PutMapping("/mastery-floor")
    public ChampionPoolDto setMasteryFloor(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @RequestBody PoolMasteryFloorRequest request) {
        return championPoolService.setMasteryFloor(userService.requireActor(actorDiscordId), teamId,
                request == null ? 0 : request.masteryFloor());
    }
}
