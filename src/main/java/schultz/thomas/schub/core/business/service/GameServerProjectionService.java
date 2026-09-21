package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;

/**
 * Choisit laquelle des trois projections d'un serveur l'acteur a le droit de voir.
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

    /** Vrai si l'acteur voit {@code deploymentId} et les ports. */
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
        return servers.stream().map(mapper::toDto).toList();
    }

    public Object project(GameServer server, User actor) {
        return project(List.of(server), actor).get(0);
    }

    /** La forme infra d'un serveur — pour les réponses d'écriture. */
    public GameServerDto toInfraDto(GameServer server) {
        return mapper.toDto(server);
    }
}
