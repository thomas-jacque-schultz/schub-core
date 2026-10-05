package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.User;

// Le rôle ouvre la porte (SERVER_START/STOP) ; la liste des référents la restreint, sauf SERVER_CONTROL_ALL.
@Service
@RequiredArgsConstructor
public class GameServerControlPolicy {

    private final PermissionEvaluator permissionEvaluator;

    public boolean canControl(User actor, GameServer server) {
        return actor != null
                && permissionEvaluator.can(actor, Permission.SERVER_START, null)
                && permissionEvaluator.can(actor, Permission.SERVER_STOP, null)
                && isReferentOrControlsAll(actor, server);
    }

    public void require(User actor, GameServer server, Permission permission) {
        permissionEvaluator.require(actor, permission, null);
        if (!isReferentOrControlsAll(actor, server)) {
            throw new NotReferentException(server.getSlug());
        }
    }

    private boolean isReferentOrControlsAll(User actor, GameServer server) {
        return permissionEvaluator.can(actor, Permission.SERVER_CONTROL_ALL, null)
                || (server.getReferents() != null && server.getReferents().contains(actor.getId()));
    }
}
