package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ouvre les permissions du chantier D aux rôles système déjà semés.
 *
 * <h2>Pourquoi une migration, et pas seulement l'enum</h2>
 *
 * <p>{@link SystemRole} dit ce que chaque rôle <em>devrait</em> porter, mais les rôles vivent en
 * base et n'y sont écrits qu'une fois, par {@code V003}. Sur une base déjà migrée, une permission
 * ajoutée à l'enum n'apparaît donc nulle part : elle existerait dans le code, serait vérifiée par
 * le code, et ne serait accordée à personne. C'est le genre de décalage qui ne lève aucune erreur
 * et se diagnostique en une heure.</p>
 *
 * <h2>Ce qu'elle ajoute, et à qui</h2>
 *
 * <ul>
 *   <li>{@code VISITEUR} reçoit {@code TEAM_CREATE} — la décision du plan §D.2 bis. Sans elle,
 *       personne ne peut rien faire de l'outil, ce qui viderait le chantier de son sens.</li>
 *   <li>{@code MODERATOR} la reçoit aussi : sinon une promotion <em>retirerait</em> à quelqu'un
 *       le droit de créer une équipe.</li>
 *   <li>{@code ADMINISTRATOR} et {@code OWNER} reçoivent les quatre, parce qu'ils sont définis
 *       comme « tout » et « tout sauf {@code ROLE_MANAGE} ».</li>
 * </ul>
 *
 * <h2>Ce qu'elle ne fait surtout pas</h2>
 *
 * <p><strong>Elle n'aligne pas les rôles sur l'enum, elle ajoute ce qui manque.</strong> Un rôle
 * système est indestructible mais pas immuable : ses permissions sont éditables depuis l'écran
 * des rôles, c'est la fenêtre demandée. Réécrire la liste entière effacerait sans bruit un
 * réglage fait à la main. Les rôles créés par l'utilisateur ne sont pas touchés du tout : c'est
 * à lui de décider qui, chez lui, crée des équipes.</p>
 */
@ChangeUnit(id = "team-permissions-seed", order = "005", author = "schub")
public class V005_TeamPermissionsSeed {

    private static final Logger log = LoggerFactory.getLogger(V005_TeamPermissionsSeed.class);

    /** Les permissions apparues avec le chantier D — les seules que cette migration distribue. */
    private static final Set<Permission> NOUVELLES = EnumSet.of(
            Permission.TEAM_CREATE,
            Permission.TEAM_VIEW,
            Permission.TEAM_EDIT,
            Permission.COMPOSITION_EDIT);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var roles = mongoTemplate.getCollection("roles");
        for (SystemRole role : SystemRole.values()) {
            Document document = roles.find(Filters.eq("name", role.roleName())).first();
            if (document == null) {
                log.warn("Rôle système {} absent — V003 le sèmera avec les permissions à jour", role.roleName());
                continue;
            }
            List<String> presentes = document.getList("permissions", String.class);
            List<String> aAjouter = new ArrayList<>();
            for (Permission permission : NOUVELLES) {
                if (role.permissions().contains(permission)
                        && (presentes == null || !presentes.contains(permission.name()))) {
                    aAjouter.add(permission.name());
                }
            }
            if (aAjouter.isEmpty()) {
                continue;
            }
            roles.updateOne(Filters.eq("name", role.roleName()),
                    Updates.addEachToSet("permissions", aAjouter));
            log.info("Rôle système {} : {} ajoutée(s)", role.roleName(), aAjouter);
        }
    }

    /**
     * Retire les quatre permissions des rôles système, et d'eux seuls.
     *
     * <p>Un retour en arrière parfait est impossible : si quelqu'un a coché {@code TEAM_VIEW} à
     * la main sur {@code MODERATOR} entre-temps, ce retrait l'efface. C'est assumé — l'inverse
     * laisserait des droits sur des permissions que le code d'une version antérieure ne connaît
     * pas.</p>
     */
    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        List<String> noms = NOUVELLES.stream().map(Permission::name).toList();
        mongoTemplate.getCollection("roles").updateMany(
                Filters.eq("system", true),
                Updates.pullAll("permissions", noms));
    }
}
