package schultz.thomas.schub.core.lol.riot.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.lol.riot.api.dto.ChampionGridDto;
import schultz.thomas.schub.core.lol.riot.api.dto.IngestPauseDto;
import schultz.thomas.schub.core.lol.riot.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.lol.riot.api.dto.RiotDataInvalidationDto;
import schultz.thomas.schub.core.lol.riot.api.dto.RiotDataInventoryDto;
import schultz.thomas.schub.core.lol.riot.business.model.ChampionGrid;
import schultz.thomas.schub.core.lol.riot.business.model.IngestPauseState;
import schultz.thomas.schub.core.lol.riot.business.model.ReferenceGrid;
import schultz.thomas.schub.core.lol.riot.business.model.RiotDataInvalidation;
import schultz.thomas.schub.core.lol.riot.business.model.RiotDataInventory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RiotDtoMapper {

    ChampionGridDto toDto(ChampionGrid grid);

    ReferenceGridDto toDto(ReferenceGrid grid);

    IngestPauseDto toDto(IngestPauseState pause);

    RiotDataInventoryDto toDto(RiotDataInventory inventory);

    RiotDataInvalidationDto toDto(RiotDataInvalidation invalidation);
}
