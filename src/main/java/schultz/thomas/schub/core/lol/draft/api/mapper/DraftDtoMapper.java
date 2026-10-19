package schultz.thomas.schub.core.lol.draft.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.lol.draft.api.dto.CompositionRecordDto;
import schultz.thomas.schub.core.lol.draft.api.dto.DraftAdviceDto;
import schultz.thomas.schub.core.lol.draft.api.dto.TierCombosDto;
import schultz.thomas.schub.core.lol.draft.business.model.CompositionRecord;
import schultz.thomas.schub.core.lol.draft.business.model.DraftAdvice;
import schultz.thomas.schub.core.lol.draft.business.model.TierCombos;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DraftDtoMapper {

    DraftAdviceDto toDto(DraftAdvice advice);

    List<CompositionRecordDto> toCompositionRecordDtos(List<CompositionRecord> records);

    TierCombosDto toDto(TierCombos combos);
}
