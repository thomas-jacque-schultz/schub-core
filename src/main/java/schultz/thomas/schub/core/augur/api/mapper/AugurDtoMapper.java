package schultz.thomas.schub.core.augur.api.mapper;

import org.mapstruct.Mapper;
import schultz.thomas.schub.core.augur.api.dto.FindingDto;
import schultz.thomas.schub.core.augur.api.dto.ImpactDto;
import schultz.thomas.schub.core.augur.api.dto.PatternDto;
import schultz.thomas.schub.core.augur.api.dto.PatternRequest;
import schultz.thomas.schub.core.augur.business.model.Finding;
import schultz.thomas.schub.core.augur.business.model.Impact;
import schultz.thomas.schub.core.augur.business.model.PatternDraft;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AugurDtoMapper {

    ImpactDto toDto(Impact impact);

    PatternDraft toDraft(PatternRequest request);

    default PatternDto toDto(PatternVersion pattern) {
        return PatternDto.of(pattern);
    }

    default List<PatternDto> toPatternDtos(List<PatternVersion> patterns) {
        return patterns.stream().map(this::toDto).toList();
    }

    default List<FindingDto> toFindingDtos(List<Finding> findings) {
        return findings.stream().map(f -> FindingDto.of(f.record(), f.pattern())).toList();
    }
}
