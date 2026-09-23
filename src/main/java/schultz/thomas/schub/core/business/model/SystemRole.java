package schultz.thomas.schub.core.business.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum SystemRole {

    VISITEUR(EnumSet.of(Permission.SERVER_VIEW, Permission.TEAM_CREATE)),

    MODERATOR(EnumSet.of(Permission.SERVER_VIEW, Permission.SERVER_START, Permission.SERVER_STOP,
            Permission.TEAM_CREATE)),

    // Liste explicite, jamais complementOf : une nouvelle permission ne doit arriver ici que si on l'écrit.
    ADMINISTRATOR(EnumSet.of(
            Permission.SERVER_VIEW, Permission.SERVER_INFRA_VIEW,
            Permission.SERVER_START, Permission.SERVER_STOP,
            Permission.SERVER_CREATE, Permission.SERVER_EDIT, Permission.SERVER_DELETE,
            Permission.PORT_VIEW, Permission.PORT_RULE_EDIT,
            Permission.DISCORD_CHANNEL_MANAGE,
            Permission.USER_VIEW, Permission.USER_ROLE_ASSIGN,
            Permission.TEAM_CREATE)),

    OWNER(EnumSet.allOf(Permission.class));

    private final Set<Permission> permissions;

    SystemRole(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    public String roleName() {
        return name();
    }
}
