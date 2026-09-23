package schultz.thomas.schub.core.business.mapper;

import schultz.thomas.schub.core.api.dto.StaticPortRuleDto;
import schultz.thomas.schub.core.api.dto.StaticPortRuleRequest;
import schultz.thomas.schub.core.data.model.StaticPortRuleEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface StaticPortRuleMapper {

    @Mapping(target = "id", ignore = true)
    StaticPortRuleEntity toEntity(StaticPortRuleRequest request);

    StaticPortRuleDto toDto(StaticPortRuleEntity entity);

    List<StaticPortRuleDto> toDtos(List<StaticPortRuleEntity> entities);
}
