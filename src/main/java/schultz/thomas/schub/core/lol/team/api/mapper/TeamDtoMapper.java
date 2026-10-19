package schultz.thomas.schub.core.lol.team.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.lol.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.lol.team.business.model.ChampionPool;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TeamDtoMapper {

    ChampionPoolDto toDto(ChampionPool pool);
}
