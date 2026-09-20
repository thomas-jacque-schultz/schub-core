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

    ADMINISTRATOR(EnumSet.complementOf(EnumSet.of(Permission.ROLE_MANAGE))),

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
