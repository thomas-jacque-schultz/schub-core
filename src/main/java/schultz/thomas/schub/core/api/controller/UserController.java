package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.AssignRoleRequest;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.api.dto.RiotAccountRequest;
import schultz.thomas.schub.core.api.dto.RiotAccountSuggestionDto;
import schultz.thomas.schub.core.api.dto.UserDto;
import schultz.thomas.schub.core.api.dto.UserIdentityDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceRef;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotAccountService;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Set;

/**
 * Les comptes. Ces routes vivaient dans le connecteur Discord jusqu'au 18-09 ; elles sont ici
 * parce que l'identité est du domaine (plan §1).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;
    private final RiotAccountService riotAccountService;

    @GetMapping
    public List<UserDto> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        User actor = userService.requireActor(actorDiscordId);
        permissionEvaluator.require(actor, Permission.USER_VIEW, null);
        return userService.toDtos(userService.findAll());
    }

    /**
     * Qui est ce compte Discord, et que peut-il faire ? — <strong>le point d'entrée de la
     * connexion</strong>, appelé par le BFF après l'échange OAuth et par le connecteur Discord
     * avant d'exécuter une commande.
     *
     * <p><strong>Cette route crée le compte s'il est inconnu</strong>, au rôle {@code VISITEUR} :
     * tout le monde peut se connecter sans inscription préalable. Un {@code GET} qui écrit est
     * inhabituel et c'est délibéré — l'opération est idempotente et c'est la seule forme qui
     * convienne au retour d'une redirection OAuth, qui est un {@code GET} de navigateur.</p>
     *
     * <p>Elle n'exige aucune permission : elle <em>établit</em> l'identité, elle ne peut donc pas
     * la présupposer. Le secret interne reste ce qui la protège.</p>
     *
     * <p>{@code discordUsername} et {@code avatarUrl} sont facultatifs et viennent de l'appelant,
     * seul à avoir parlé à Discord : le cœur ne connaît pas l'API Discord et ne doit pas
     * l'apprendre.</p>
     */
    @GetMapping("/by-discord/{discordId}")
    public UserIdentityDto byDiscordId(@PathVariable String discordId,
                                       @RequestParam(required = false) String discordUsername,
                                       @RequestParam(required = false) String avatarUrl) {
        User user = userService.findOrCreateByDiscordId(discordId, discordUsername, avatarUrl);
        riotAccountService.resolvePendingLink(user);
        return userService.toIdentityDto(user);
    }

    /**
     * Les permissions <em>effectives</em> de ce compte, éventuellement sur une ressource précise.
     *
     * <p>Sert au connecteur Discord, qui a cessé de juger lui-même : il pose la question, le cœur
     * répond, et il n'existe plus qu'une seule règle. Avec {@code gameServer}, la réponse inclut
     * ce que l'acteur tient d'être administrateur de <em>ce</em> serveur — ce qu'un jeu de
     * permissions de rôle seul ne dirait pas.</p>
     */
    @GetMapping("/by-discord/{discordId}/permissions")
    public Set<Permission> effectivePermissions(@PathVariable String discordId,
                                                @RequestParam(required = false) String discordUsername,
                                                @RequestParam(required = false) String gameServer) {
        User user = userService.findOrCreateByDiscordId(discordId, discordUsername, null);
        return permissionEvaluator.effectivePermissions(user, ResourceRef.gameServer(gameServer));
    }

    // --- le lien vers le compte Riot ---
    //
    // Pas de DELETE : on change de compte, on ne délie pas. Délier laisserait des places
    // d'effectif rattachées à quelqu'un qui n'a plus de compte de jeu, donc des équipes dans un
    // état qu'aucun écran ne sait expliquer.
    //
    // Toutes sous `/users/me` : l'acteur de l'en-tête X-Actor-Id est le sujet de la route, et
    // il n'existe aucun chemin vers le lien de quelqu'un d'autre. Ce n'est pas une permission
    // qu'on vérifie, c'est une route qui n'a pas de second paramètre — la forme la plus solide
    // de « lui, et personne d'autre », parce qu'il n'y a rien à oublier de contrôler.
    //
    // Rappel écrit en toutes lettres dans RiotAccountService : la propriété du compte Riot
    // n'est PAS vérifiée. RSO demande une approbation Riot séparée, hors périmètre.

    /**
     * Ce que l'appelant a déclaré, où en est la résolution du {@code puuid}, et où en est la
     * collecte de ses parties — {@code ingest} reste nul si le connecteur ne répond pas.
     */
    @GetMapping("/me/riot-account")
    public RiotAccountDto myRiotAccount(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        return riotAccountService.of(userService.requireActor(actorDiscordId));
    }

    /**
     * Déclare, relance ou remplace le Riot ID de l'appelant, sous la forme {@code Pseudo#TAG}.
     *
     * <p>{@code PUT} et non {@code POST} : c'est le remplacement d'une sous-ressource qui existe
     * en un seul exemplaire, et l'opération est idempotente — rejouer le même Riot ID relance la
     * résolution du {@code puuid}, ce qui est exactement le « réessayer » dont un écran a besoin
     * quand le connecteur Riot était éteint.</p>
     *
     * <p>400 si le Riot ID est malformé, 409 si ce compte Riot est déjà revendiqué ailleurs.
     * <strong>409 également quand il s'agit d'en remplacer un autre sans
     * {@code confirmChange}</strong> : le corps porte alors un objet {@code change} qui dit ce
     * que le remplacement emporte — statistiques personnelles remises à zéro, nouvelle collecte,
     * places d'effectif à revendiquer — et c'est de quoi poser la question à l'écran. Rejouer
     * avec {@code confirmChange: true} applique le changement, et la réponse reporte le même
     * objet {@code change}.</p>
     *
     * <p><strong>Pas d'erreur si le connecteur est injoignable</strong> : la déclaration est
     * conservée en {@code EN_ATTENTE_DE_RESOLUTION}, et c'est {@code state} qui le dit.</p>
     *
     * <p>Le front enchaîne sur {@code POST /teams/claim} après un succès : c'est lui qui rattache
     * l'appelant aux places d'effectif qui l'attendaient, et qui les resynchronise après un
     * changement de compte.</p>
     */
    @PutMapping("/me/riot-account")
    public RiotAccountDto linkMyRiotAccount(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody RiotAccountRequest request) {
        return riotAccountService.link(userService.requireActor(actorDiscordId),
                request.riotId(), request.confirmChange());
    }

    /**
     * Les comptes connus qui ressemblent à cette saisie — la seule route de l'écran de liaison
     * avant le clic.
     *
     * <p>Elle existe parce que l'API Riot <strong>ne sait pas chercher par pseudo partiel</strong> :
     * {@code account-v1} ne résout qu'un {@code gameName#tagLine} exact. Les propositions viennent
     * donc de l'index du connecteur — joueurs croisés dans nos parties, et comptes qu'une
     * vérification a fait confirmer par Riot.</p>
     *
     * <p><strong>Une saisie partielle cherche ; un {@code Pseudo#TAG} complet vérifie.</strong>
     * Dans le second cas, le cœur fait d'abord résoudre ce Riot ID par le connecteur, ce qui
     * l'inscrit dans l'index : le compte revient ensuite dans la liste comme n'importe quel
     * autre, et c'est ce qui rend l'écran praticable quand rien n'a encore été collecté. La
     * vérification <em>ne lie rien</em> — seul {@code PUT /users/me/riot-account} lie.</p>
     *
     * <p>Liste vide si rien ne ressemble : ce n'est pas une erreur. 404 si Riot ne connaît pas un
     * Riot ID complet, 503 si le connecteur ne répond pas — là, le silence serait un mensonge.</p>
     */
    @GetMapping("/me/riot-account/suggestions")
    public List<RiotAccountSuggestionDto> suggestRiotAccounts(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return riotAccountService.suggestions(userService.requireActor(actorDiscordId), q, limit);
    }


    /** Attribution d'un rôle. Les règles anti-élévation sont dans le service, pas ici. */
    @PutMapping("/{id}/role")
    public UserDto assignRole(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
                              @PathVariable String id,
                              @RequestBody AssignRoleRequest request) {
        User actor = userService.requireActor(actorDiscordId);
        return userService.toDto(userService.assignRole(actor, id, request.roleId()));
    }
}
