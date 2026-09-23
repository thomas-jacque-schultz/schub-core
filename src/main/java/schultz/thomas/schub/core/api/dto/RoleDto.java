package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Permission;

import java.util.Set;

public record RoleDto(
        String id,
        String name,
        Set<Permission> permissions,
        boolean system
) {
}
