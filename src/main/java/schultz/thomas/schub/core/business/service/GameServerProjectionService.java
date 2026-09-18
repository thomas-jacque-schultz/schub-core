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
 * Choisit laquelle des trois projections d'un serveur l'acteur a le droit de voir, et résout les
 * administrateurs en pseudos.
 *
 * <p>Le choix est fait <em>ici</em> et pas dans le contrôleur pour une raison simple : c'est une
 * règle de domaine — « qui voit quoi » — et elle doit être la même pour les trois routes qui
 * renvoient un serveur. Répétée trois fois dans les contrôleurs, elle finirait par diverger sur
 * l'une d'elles, et la divergence ne se verrait pas.</p>
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
            return servers.stream().map(mapper::toMemberDto).toList();
        }
        Map<String, User> admins = resolveAdmins(servers);
        return servers.stream().map(server -> withAdmins(server, admins)).toList();
    }

    public Object project(GameServer server, User actor) {
        return project(List.of(server), actor).get(0);
    }

    /** La forme infra d'un serveur, admins résolus — pour les réponses d'écriture. */
    public GameServerDto toInfraDto(GameServer server) {
        return withAdmins(server, resolveAdmins(List.of(server)));
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
    private GameServerDto withAdmins(GameServer server, Map<String, User> resolved) {
        GameServerDto dto = mapper.toDto(server);
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
                dto.statusHistory());
    }
}
