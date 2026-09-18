package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.ServerAdminDto;
import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Choisit laquelle des trois projections d'un serveur l'acteur a le droit de voir, résout les
 * administrateurs en pseudos, et lui dit s'il est lui-même administrateur de ce serveur.
 *
 * <p>Le choix est fait <em>ici</em> et pas dans le contrôleur pour une raison simple : c'est une
 * règle de domaine — « qui voit quoi » — et elle doit être la même pour les trois routes qui
 * renvoient un serveur. Répétée trois fois dans les contrôleurs, elle finirait par diverger sur
 * l'une d'elles, et la divergence ne se verrait pas.</p>
 *
 * <p><strong>Pourquoi {@code viewerIsAdmin} et pas la liste des admins pour tous</strong> : la
 * projection membre ne nomme aucun administrateur, et c'est délibéré (décision n°10). Mais un
 * compte sans {@code SERVER_INFRA_VIEW} peut parfaitement figurer dans les {@code admins} d'un
 * serveur — c'est le cas nominal de la décision n°11. Sans ce booléen, le front ne peut pas le
 * savoir : il propose démarrer/arrêter à tout le monde, et le cœur refuse après le clic.</p>
 */
@Service
@RequiredArgsConstructor
public class GameServerProjectionService {

    private final GameServerMapper mapper;
    private final PermissionEvaluator permissionEvaluator;
    private final UserService userService;

    /** Vrai si l'acteur voit {@code deploymentId}, les ports et les administrateurs. */
    public boolean seesInfrastructure(User actor) {
        return actor != null && permissionEvaluator.can(actor, Permission.SERVER_INFRA_VIEW, null);
    }

    /**
     * @return une liste de {@link GameServerDto} ou de
     *         {@link schultz.thomas.schub.core.api.dto.GameServerMemberDto} selon l'acteur. Le
     *         type varie, et c'est le sujet : deux consommateurs aux droits différents ne
     *         reçoivent pas la même forme.
     */
    public List<?> project(List<GameServer> servers, User actor) {
        if (!seesInfrastructure(actor)) {
            return servers.stream()
                    .map(server -> mapper.toMemberDto(server, isAdminOf(server, actor)))
                    .toList();
        }
        Map<String, User> admins = resolveAdmins(servers);
        return servers.stream().map(server -> withAdmins(server, admins, actor)).toList();
    }

    public Object project(GameServer server, User actor) {
        return project(List.of(server), actor).get(0);
    }

    /** La forme infra d'un serveur, admins résolus — pour les réponses d'écriture. */
    public GameServerDto toInfraDto(GameServer server, User actor) {
        return withAdmins(server, resolveAdmins(List.of(server)), actor);
    }

    /**
     * L'acteur figure-t-il dans les {@code admins} de ce serveur ?
     *
     * <p>La comparaison porte sur l'<strong>id interne</strong> du compte, jamais sur son
     * identifiant Discord : {@code GameServer.admins} contient des ids internes depuis le 18-09
     * (plan §A.4), et confondre les deux donne un booléen toujours faux — c'est exactement le
     * défaut que ce champ corrige.</p>
     */
    private boolean isAdminOf(GameServer server, User actor) {
        return actor != null
                && server.getAdmins() != null
                && server.getAdmins().contains(actor.getId());
    }

    /**
     * Un seul aller-retour vers les comptes pour toute la page, plutôt qu'un par administrateur :
     * c'est précisément ce que le front ne doit pas avoir à faire (plan §A.4).
     */
    private Map<String, User> resolveAdmins(List<GameServer> servers) {
        Set<String> ids = new HashSet<>();
        servers.stream()
                .map(GameServer::getAdmins)
                .filter(admins -> admins != null)
                .forEach(ids::addAll);
        return userService.byIds(ids);
    }

    /**
     * Un id qui ne désigne plus personne est renvoyé sans pseudo plutôt qu'omis : le silence
     * ferait disparaître un administrateur de la liste sans que personne ne s'en aperçoive,
     * alors qu'une ligne sans nom se voit et se corrige.
     */
    private GameServerDto withAdmins(GameServer server, Map<String, User> resolved, User actor) {
        GameServerDto dto = mapper.toDto(server, isAdminOf(server, actor));
        List<ServerAdminDto> admins = server.getAdmins() == null ? List.of()
                : server.getAdmins().stream()
                .map(id -> {
                    User user = resolved.get(id);
                    return user == null
                            ? new ServerAdminDto(id, null, null)
                            : new ServerAdminDto(id, user.getDiscordUsername(), user.getAvatarUrl());
                })
                .toList();
        return new GameServerDto(
                dto.id(), dto.slug(), dto.deploymentId(), dto.name(), dto.urlConnection(),
                dto.game(), dto.gameLabel(), dto.gameIconUrl(), dto.playersMax(),
                dto.installation(), dto.version(), dto.description(),
                admins,
                dto.ports(), dto.status(), dto.lastStatusCheckAt(), dto.lastStatusChangeAt(),
                dto.statusHistory(), dto.viewerIsAdmin());
    }
}
