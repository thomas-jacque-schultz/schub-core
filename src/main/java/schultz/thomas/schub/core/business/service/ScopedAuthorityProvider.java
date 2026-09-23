package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.data.model.User;

import java.util.Set;

public interface ScopedAuthorityProvider {

    ResourceType resourceType();

    Set<Permission> grantedTo(User actor, String resourceId);
}
