package schultz.thomas.schub.core.business.mapper;

import schultz.thomas.schub.core.api.dto.StaticPortRuleDto;
import schultz.thomas.schub.core.api.dto.StaticPortRuleRequest;
import schultz.thomas.schub.core.data.model.StaticPortRuleEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

/**
 * Traduction entre la règle permanente et ses contrats.
 *
 * <p>{@code id} est explicitement ignoré à l'entrée : il est attribué par la base, jamais par
 * l'appelant. {@link StaticPortRuleRequest} ne le porte déjà pas — l'annotation le redit ici pour
 * que MapStruct ne le réintroduise pas le jour où le type d'entrée changerait.</p>
 */
@Mapper(componentModel = "spring")
public interface StaticPortRuleMapper {

    @Mapping(target = "id", ignore = true)
    StaticPortRuleEntity toEntity(StaticPortRuleRequest request);

    StaticPortRuleDto toDto(StaticPortRuleEntity entity);

    List<StaticPortRuleDto> toDtos(List<StaticPortRuleEntity> entities);
}
