package schultz.thomas.schub.core.business.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Les quatre rôles livrés avec le système, et le seul endroit où leurs permissions sont écrites.
 *
 * <p>Ils reprennent l'ancien {@code UserPrivilegeEnum} du connecteur Discord pour que la reprise
 * soit un simple renommage — {@code USER} devient {@code VISITEUR}, le reste ne bouge pas.</p>
 *
 * <p><strong>Système veut dire indestructible, pas immuable.</strong> Leurs permissions restent
 * éditables depuis l'écran des rôles — c'est la fenêtre demandée — sauf {@link #OWNER}, qui est
 * le garde-fou anti-verrouillage : un OWNER amputé de ses droits par mégarde fermerait
 * l'administration à tout le monde, pour de bon.</p>
 */
public enum SystemRole {

    /**
     * {@code TEAM_CREATE} est ici et pas ailleurs (plan §D.2 bis) : un visiteur est n'importe
     * qui muni d'un compte Discord, et c'est exactement le public de l'outil d'équipe. Le lui
     * refuser reviendrait à livrer un outil que personne ne peut ouvrir.
     */
    VISITEUR(EnumSet.of(Permission.SERVER_VIEW, Permission.TEAM_CREATE)),

    /**
     * {@code TEAM_CREATE} aussi : sans elle, promouvoir quelqu'un en modérateur lui
     * <em>retirerait</em> le droit de créer une équipe. Une promotion qui ampute est un bug
     * qu'on ne découvre qu'à l'usage.
     */
    MODERATOR(EnumSet.of(Permission.SERVER_VIEW, Permission.SERVER_START, Permission.SERVER_STOP,
            Permission.TEAM_CREATE)),

    /**
     * Administrateur <strong>de l'hébergement</strong> — et de rien d'autre.
     *
     * <p>Ce rôle était défini comme « tout sauf {@code ROLE_MANAGE} ». Il héritait donc
     * mécaniquement de {@code TEAM_VIEW}, {@code TEAM_EDIT} et {@code COMPOSITION_EDIT} dès leur
     * apparition, et pouvait modifier l'effectif de <em>n'importe quelle</em> équipe. Ce n'était
     * l'intention de personne : piloter des serveurs de jeu et composer une équipe LoL sont deux
     * domaines distincts, qui ne partageaient ce rôle que par accident de définition.</p>
     *
     * <p><strong>La liste est désormais explicite, et c'est le vrai correctif.</strong> Avec
     * {@code complementOf}, <em>toute</em> permission ajoutée plus tard atterrissait ici en
     * silence — y compris celles d'un domaine qui n'existe pas encore. Une permission
     * s'accorde, elle ne se reçoit pas par défaut : ajouter une entrée à {@link Permission}
     * n'accorde plus rien à personne tant que quelqu'un ne l'a pas écrit ici.</p>
     *
     * <p>{@code TEAM_CREATE} y figure parce qu'un administrateur est aussi une personne, qui a
     * le droit de monter son équipe. Les trois autres s'obtiennent en étant capitaine ou membre
     * de l'équipe concernée, comme pour tout le monde — voir {@code TeamScopedAuthority}.</p>
     */
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

    /** Le nom du rôle en base est celui de la constante : un seul vocabulaire partout. */
    public String roleName() {
        return name();
    }
}
