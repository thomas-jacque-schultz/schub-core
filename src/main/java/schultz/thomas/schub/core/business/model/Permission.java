package schultz.thomas.schub.core.business.model;

public enum Permission {

    SERVER_VIEW,

    SERVER_INFRA_VIEW,

    SERVER_START,
    SERVER_STOP,

    SERVER_CREATE,
    SERVER_EDIT,
    SERVER_DELETE,

    PORT_VIEW,
    PORT_RULE_EDIT,

    DISCORD_CHANNEL_MANAGE,

    USER_VIEW,
    USER_ROLE_ASSIGN,

    INGEST_VIEW,

    // TEAM_CREATE est globale ; les trois suivantes s'évaluent sur une équipe (TeamScopedAuthority).
    TEAM_CREATE,

    TEAM_VIEW,

    TEAM_EDIT,

    COMPOSITION_EDIT,

    // Jamais attribuable : réservée au rôle système OWNER.
    ROLE_MANAGE
}
