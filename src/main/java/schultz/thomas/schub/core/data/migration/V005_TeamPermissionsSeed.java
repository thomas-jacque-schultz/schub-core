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
 * Les rôles vivent en base, écrits une fois par V003 : une permission ajoutée à l'enum n'est accordée
 * à personne sans migration. Ajoute ce qui manque, ne réaligne pas (les rôles système restent éditables).
 */
@ChangeUnit(id = "team-permissions-seed", order = "005", author = "schub")
public class V005_TeamPermissionsSeed {

    private static final Logger log = LoggerFactory.getLogger(V005_TeamPermissionsSeed.class);

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

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        List<String> noms = NOUVELLES.stream().map(Permission::name).toList();
        mongoTemplate.getCollection("roles").updateMany(
                Filters.eq("system", true),
                Updates.pullAll("permissions", noms));
    }
}
