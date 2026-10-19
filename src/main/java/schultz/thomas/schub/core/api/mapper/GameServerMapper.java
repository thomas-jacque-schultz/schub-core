package schultz.thomas.schub.core.api.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import schultz.thomas.schub.core.api.dto.GameServerDto;
import schultz.thomas.schub.core.api.dto.GameServerMemberDto;
import schultz.thomas.schub.core.api.dto.GameServerStatusHistoryEntryDto;
import schultz.thomas.schub.core.data.model.Game;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.GameServerStatus;
import schultz.thomas.schub.core.data.model.GameServerStatusHistoryEntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
public interface GameServerMapper {

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastStatusCheckAt", ignore = true)
    @Mapping(target = "lastStatusChangeAt", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "lastCommandFailure", ignore = true)
    @Mapping(target = "referents", source = "referentIds")
    @Mapping(target = "game", source = "game", qualifiedByName = "stringToGame")
    GameServer toEntity(GameServerDto dto);

    @Mapping(target = "game", source = "game", qualifiedByName = "gameToString")
    @Mapping(target = "gameLabel", source = "game", qualifiedByName = "gameToLabel")
    @Mapping(target = "gameIconUrl", source = "game", qualifiedByName = "gameToIcon")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "statusHistory", source = "statusHistory", qualifiedByName = "historyToDto")
    @Mapping(target = "referentIds", source = "referents")
    @Mapping(target = "referents", ignore = true)
    @Mapping(target = "viewerCanControl", ignore = true)
    GameServerDto toDto(GameServer entity);

    @Mapping(target = "game", source = "game", qualifiedByName = "gameToString")
    @Mapping(target = "gameLabel", source = "game", qualifiedByName = "gameToLabel")
    @Mapping(target = "gameIconUrl", source = "game", qualifiedByName = "gameToIcon")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "statusHistory", source = "statusHistory", qualifiedByName = "historyToDto")
    @Mapping(target = "referents", ignore = true)
    @Mapping(target = "viewerCanControl", ignore = true)
    GameServerMemberDto toMemberDto(GameServer entity);

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
