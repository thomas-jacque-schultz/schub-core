package schultz.thomas.schub.core.augur.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.augur.api.dto.EvaluationDto;
import schultz.thomas.schub.core.augur.api.dto.FindingDto;
import schultz.thomas.schub.core.augur.api.dto.ImpactDto;
import schultz.thomas.schub.core.augur.api.dto.PatternDto;
import schultz.thomas.schub.core.augur.api.dto.PatternRequest;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.business.model.Finding;
import schultz.thomas.schub.core.augur.business.model.Impact;
import schultz.thomas.schub.core.augur.business.model.PatternDraft;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AugurDtoMapper {

    ImpactDto toDto(Impact impact);

    PatternDraft toDraft(PatternRequest request);

    PatternDto toDto(PatternVersion pattern);

    default List<PatternDto> toPatternDtos(List<PatternVersion> patterns) {
        return patterns.stream().map(this::toDto).toList();
    }

    List<FindingDto> toFindingDtos(List<Finding> findings);

    @Mapping(target = "pattern", source = "record.patternKey")
    @Mapping(target = "label", source = "pattern.label")
    @Mapping(target = "sentence", source = "pattern.sentence")
    @Mapping(target = "polarity", source = "pattern.polarity")
    @Mapping(target = "category", source = "pattern.category")
    @Mapping(target = "nature", source = "pattern.nature")
    @Mapping(target = "confidence", source = "record.degree")
    @Mapping(target = "experimental", source = "pattern.experimental")
    @Mapping(target = "limits", source = "pattern.limits")
    @Mapping(target = "evidence", source = "record.conditions")
    @Mapping(target = "context", source = "record.context")
    FindingDto toDto(Finding finding);

    List<EvaluationDto> toEvaluationDtos(List<Evaluator.Evaluation> evaluations);
}
