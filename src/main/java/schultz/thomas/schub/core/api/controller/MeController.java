package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.DisplayNameRequest;
import schultz.thomas.schub.core.api.dto.MeDto;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

/**
 * Le compte de l'appelant, vu par lui-même.
 *
 * <p>Comme les routes {@code /users/me}, celle-ci n'a pas de second paramètre : l'acteur de
 * {@code X-Actor-Id} <em>est</em> le sujet, et il n'existe aucun chemin vers le profil de
 * quelqu'un d'autre. C'est la forme la plus solide de « lui, et personne d'autre » — il n'y a
 * rien à oublier de contrôler.</p>
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    /**
     * Identité, rôle, permissions et compte Riot d'un seul appel.
     *
     * <p>Le connecteur Riot injoignable laisse {@code riot.ingest} à {@code null} et ne fait pas
     * échouer la réponse : un profil qui ne s'affiche plus parce qu'une API tierce est éteinte
     * serait une panne bien plus grave que l'absence d'un compteur.</p>
     */
    @GetMapping
    public MeDto me(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        return userService.toMeDto(userService.requireActor(actorDiscordId));
    }

    /**
     * Change le nom affiché sur le site.
     *
     * <p>Un corps vide rend le pseudo Discord. 400 si le nom est trop long ou ne contient rien
     * d'affichable ; jamais 409 — ce nom n'est pas unique, voir {@code User#displayName}.</p>
     */
    @PutMapping("/display-name")
    public MeDto changeDisplayName(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody DisplayNameRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return userService.toMeDto(userService.changeDisplayName(actor, request.displayName()));
    }
}
