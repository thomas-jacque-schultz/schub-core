package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.data.model.Permission;

import java.util.Set;

public record UserIdentityDto(
        UserDto user,
        Set<Permission> permissions
) {
}
