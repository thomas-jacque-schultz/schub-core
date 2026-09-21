package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.team.api.dto.GameReviewDto;
import schultz.thomas.schub.core.team.api.dto.GameReviewRequest;
import schultz.thomas.schub.core.team.api.dto.GameReviewsDto;
import schultz.thomas.schub.core.team.business.service.GameReviewService;

/**
 * La revue d'après-match, partie par partie (plan §D.10).
 *
 * <p>Sous {@code /teams/{teamId}/games/{matchId}} : une note n'existe pas sans son équipe ni sans
 * sa partie, et c'est l'équipe qui porte les droits. Le chemin porte les deux, donc l'appartenance
 * se vérifie sans avoir à la retrouver.</p>
 */
@RestController
@RequestMapping("/teams/{teamId}/games/{matchId}/reviews")
@RequiredArgsConstructor
public class GameReviewController {

    private final GameReviewService reviewService;
    private final UserService userService;

    @GetMapping
    public GameReviewsDto ofGame(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String matchId) {
        return reviewService.ofGame(userService.requireActor(actorDiscordId), teamId, matchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameReviewDto create(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String matchId,
            @RequestBody GameReviewRequest request) {
        return reviewService.create(userService.requireActor(actorDiscordId), teamId, matchId,
                request.subjectMemberId(), request.content());
    }

    @PutMapping("/{reviewId}")
    public GameReviewDto update(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String matchId,
            @PathVariable String reviewId,
            @RequestBody GameReviewRequest request) {
        return reviewService.update(userService.requireActor(actorDiscordId), teamId, matchId,
                reviewId, request.content());
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String teamId,
            @PathVariable String matchId,
            @PathVariable String reviewId) {
        reviewService.delete(userService.requireActor(actorDiscordId), teamId, matchId, reviewId);
    }
}
