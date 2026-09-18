package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import schultz.thomas.schub.core.business.model.Permission;

import java.util.EnumSet;
import java.util.Set;

/**
 * Un nom et un jeu de permissions, éditable en base.
 *
 * <p>C'est la moitié souple du modèle : les {@link Permission} sont figées dans le code parce
 * qu'elles correspondent à des vérifications, les rôles se composent librement.</p>
 */
@Data
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    /**
     * Un rôle système ne se supprime pas et ne se renomme pas.
     *
     * <p>Sans cette marque, supprimer {@code VISITEUR} laisserait tous les comptes créés à la
     * connexion pointer vers un rôle inexistant — donc sans aucun droit, sans message d'erreur,
     * et sans moyen évident de comprendre pourquoi.</p>
     */
    private boolean system;
}
