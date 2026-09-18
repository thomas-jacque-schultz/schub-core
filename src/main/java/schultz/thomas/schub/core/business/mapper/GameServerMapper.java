package schultz.thomas.schub.core.business.mapper;

import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.GameServerMemberDto;
import schultz.thomas.schub.core.api.dto.GameServerStatusHistoryEntryDto;
import schultz.thomas.schub.core.api.dto.ServerAdminDto;
import schultz.thomas.schub.core.data.model.Game;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.GameServerStatus;
import schultz.thomas.schub.core.data.model.GameServerStatusHistoryEntry;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Traduction entre l'entité et ses contrats.
 *
 * <p>L'état observé — statut, dates, historique — est systématiquement ignoré en entrée : il
 * appartient à la boucle d'observation, pas à celui qui édite la fiche. Sans cette exclusion,
 * un PUT depuis l'interface effacerait l'historique d'état du serveur.</p>
 *
 * <p>{@code admins} est ignoré en sortie et rempli par
 * {@code GameServerProjectionService} : résoudre un id interne en pseudo et avatar demande un
 * aller-retour vers les comptes, ce qu'un mapper ne fait pas. En entrée, à l'inverse, seul le
 * {@code userId} est retenu — le reste du DTO est de l'affichage.</p>
 */
@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
public interface GameServerMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastStatusCheckAt", ignore = true)
    @Mapping(target = "lastStatusChangeAt", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    void updateFromSource(GameServer source, @MappingTarget GameServer target);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastStatusCheckAt", ignore = true)
    @Mapping(target = "lastStatusChangeAt", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "game", source = "game", qualifiedByName = "stringToGame")
    @Mapping(target = "admins", source = "admins", qualifiedByName = "adminsToIds")
    GameServer toEntity(GameServerDto dto);

    /** Projection infra : tout, y compris déploiement, ports et administrateurs. */
    @Mapping(target = "admins", ignore = true)
    @Mapping(target = "game", source = "game", qualifiedByName = "gameToString")
    @Mapping(target = "gameLabel", source = "game", qualifiedByName = "gameToLabel")
    @Mapping(target = "gameIconUrl", source = "game", qualifiedByName = "gameToIcon")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "statusHistory", source = "statusHistory", qualifiedByName = "historyToDto")
    GameServerDto toDto(GameServer entity);

    /** Projection membre : de quoi rejoindre et suivre, rien qui décrive l'infrastructure. */
    @Mapping(target = "game", source = "game", qualifiedByName = "gameToString")
    @Mapping(target = "gameLabel", source = "game", qualifiedByName = "gameToLabel")
    @Mapping(target = "gameIconUrl", source = "game", qualifiedByName = "gameToIcon")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "statusHistory", source = "statusHistory", qualifiedByName = "historyToDto")
    GameServerMemberDto toMemberDto(GameServer entity);

    /** Tolère le nom technique comme le libellé : « MINECRAFT » et « Minecraft » désignent le même jeu. */
    @Named("stringToGame")
    default Game stringToGame(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(Game.values())
                .filter(game -> game.name().equalsIgnoreCase(value) || game.getLabel().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    @Named("gameToString")
    default String gameToString(Game value) {
        return value != null ? value.name() : null;
    }

    @Named("gameToLabel")
    default String gameToLabel(Game value) {
        return value != null ? value.getLabel() : null;
    }

    @Named("gameToIcon")
    default String gameToIcon(Game value) {
        return value != null ? value.getIconUrl() : null;
    }

    @Named("statusToString")
    default String statusToString(GameServerStatus value) {
        return value != null ? value.name() : null;
    }

    /**
     * Seul l'{@code userId} traverse. Renvoyer {@code null} sur une liste absente est
     * volontaire : combiné à {@code NullValuePropertyMappingStrategy.IGNORE}, c'est ce qui fait
     * qu'un PUT sans le champ {@code admins} laisse la liste existante intacte au lieu de la vider.
     */
    @Named("adminsToIds")
    default List<String> adminsToIds(List<ServerAdminDto> value) {
        if (value == null) {
            return null;
        }
        return value.stream()
                .map(ServerAdminDto::userId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    @Named("historyToDto")
    default List<GameServerStatusHistoryEntryDto> historyToDto(List<GameServerStatusHistoryEntry> value) {
        if (value == null) {
            return Collections.emptyList();
        }
        return value.stream().map(this::entryToDto).toList();
    }

    default GameServerStatusHistoryEntryDto entryToDto(GameServerStatusHistoryEntry value) {
        if (value == null) {
            return null;
        }
        return new GameServerStatusHistoryEntryDto(statusToString(value.getStatus()), value.getStartedAt());
    }
}
