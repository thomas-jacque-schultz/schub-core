package schultz.thomas.schub.core.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import schultz.thomas.schub.core.dto.GameServerDto;
import schultz.thomas.schub.core.dto.GameServerStatusHistoryEntryDto;
import schultz.thomas.schub.core.model.Game;
import schultz.thomas.schub.core.model.GameServer;
import schultz.thomas.schub.core.model.GameServerStatus;
import schultz.thomas.schub.core.model.GameServerStatusHistoryEntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Traduction entre l'entité et son contrat.
 *
 * <p>L'état observé — statut, dates, historique — est systématiquement ignoré en entrée : il
 * appartient à la boucle d'observation, pas à celui qui édite la fiche. Sans cette exclusion,
 * un PUT depuis l'interface effacerait l'historique d'état du serveur.</p>
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
    GameServer toEntity(GameServerDto dto);

    @Mapping(target = "game", source = "game", qualifiedByName = "gameToString")
    @Mapping(target = "gameLabel", source = "game", qualifiedByName = "gameToLabel")
    @Mapping(target = "gameIconUrl", source = "game", qualifiedByName = "gameToIcon")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "statusHistory", source = "statusHistory", qualifiedByName = "historyToDto")
    GameServerDto toDto(GameServer entity);

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
