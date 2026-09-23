package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameServerProjectionService {

    private final GameServerMapper mapper;
    private final PermissionEvaluator permissionEvaluator;

    public boolean seesInfrastructure(User actor) {
        return actor != null && permissionEvaluator.can(actor, Permission.SERVER_INFRA_VIEW, null);
    }

    public List<?> project(List<GameServer> servers, User actor) {
        if (!seesInfrastructure(actor)) {
            return servers.stream().map(mapper::toMemberDto).toList();
        }
        return servers.stream().map(mapper::toDto).toList();
    }

    public Object project(GameServer server, User actor) {
        return project(List.of(server), actor).get(0);
    }

    public GameServerDto toInfraDto(GameServer server) {
        return mapper.toDto(server);
    }
}
