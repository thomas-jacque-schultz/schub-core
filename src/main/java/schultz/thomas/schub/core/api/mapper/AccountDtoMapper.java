package schultz.thomas.schub.core.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.api.dto.RiotAccountChangeDto;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.api.dto.RiotAccountSuggestionDto;
import schultz.thomas.schub.core.api.dto.RoleDto;
import schultz.thomas.schub.core.api.dto.UserStatsDto;
import schultz.thomas.schub.core.business.model.RiotAccount;
import schultz.thomas.schub.core.business.model.RiotAccountChange;
import schultz.thomas.schub.core.business.model.RiotAccountSuggestion;
import schultz.thomas.schub.core.business.model.UserStats;
import schultz.thomas.schub.core.data.model.Role;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountDtoMapper {

    RiotAccountDto toDto(RiotAccount account);

    RiotAccountChangeDto toDto(RiotAccountChange change);

    List<RiotAccountSuggestionDto> toSuggestionDtos(List<RiotAccountSuggestion> suggestions);

    UserStatsDto toDto(UserStats stats);

    RoleDto toDto(Role role);

    List<RoleDto> toRoleDtos(List<Role> roles);
}
