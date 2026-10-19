package schultz.thomas.schub.core.lol.team.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.lol.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.lol.team.api.dto.TrainingSlotDto;
import schultz.thomas.schub.core.lol.team.business.model.ChampionPool;
import schultz.thomas.schub.core.lol.team.data.model.TrainingSlot;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TeamDtoMapper {

    ChampionPoolDto toDto(ChampionPool pool);

    List<TrainingSlotDto> toSlotDtos(List<TrainingSlot> slots);

    List<TrainingSlot> fromSlotDtos(List<TrainingSlotDto> slots);
}
