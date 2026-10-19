package schultz.thomas.schub.core.api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.GameServerReferentDto;
import schultz.thomas.schub.core.business.service.GameServerControlPolicy;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameServerProjection {

    private final GameServerMapper mapper;
    private final PermissionEvaluator permissionEvaluator;
    private final GameServerControlPolicy controlPolicy;
    private final UserService userService;

    public boolean seesInfrastructure(User actor) {
        return actor != null && permissionEvaluator.can(actor, Permission.SERVER_INFRA_VIEW, null);
    }

    public List<?> project(List<GameServer> servers, User actor) {
        Map<String, User> referents = userService.byIds(servers.stream()
                .map(GameServer::getReferents)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        if (!seesInfrastructure(actor)) {
            return servers.stream()
                    .map(server -> mapper.toMemberDto(server)
                            .forViewer(referentsOf(server, referents), controlPolicy.canControl(actor, server)))
                    .toList();
        }
        return servers.stream()
                .map(server -> mapper.toDto(server)
                        .forViewer(referentsOf(server, referents), controlPolicy.canControl(actor, server)))
                .toList();
    }

    private List<GameServerReferentDto> referentsOf(GameServer server, Map<String, User> users) {
        if (server.getReferents() == null) {
            return List.of();
        }
        return server.getReferents().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .map(user -> new GameServerReferentDto(user.getId(), userService.displayNameOf(user), user.getDiscordId()))
                .toList();
    }

    public Object project(GameServer server, User actor) {
        return project(List.of(server), actor).get(0);
    }

    public GameServerDto toInfraDto(GameServer server, User actor) {
        Map<String, User> referents = userService.byIds(server.getReferents() == null ? Set.of() : Set.copyOf(server.getReferents()));
        return mapper.toDto(server).forViewer(referentsOf(server, referents), controlPolicy.canControl(actor, server));
    }
}
