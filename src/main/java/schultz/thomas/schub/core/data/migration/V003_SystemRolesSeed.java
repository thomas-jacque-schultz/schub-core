package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;

import java.util.Arrays;
import java.util.List;

/**
 * Crée les quatre rôles système : {@code VISITEUR}, {@code MODERATOR}, {@code ADMINISTRATOR},
 * {@code OWNER}.
 *
 * <p><strong>Rien n'est repris de {@code discordbot.users}, et c'est la décision.</strong> Le
 * cœur se connecte à la base {@code servers} avec un utilisateur Mongo qui n'a pas le droit de
 * lire {@code discordbot} — et ne doit pas l'obtenir : ce droit ne servirait qu'une fois et
 * resterait acquis pour toujours (plan §1, migration §6 bis). La prod est vierge, vérifiée le
 * 17-09 : il n'y a rien à reprendre. Les quelques comptes de dev se resèment à la connexion.</p>
 *
 * <p>L'utilisateur {@code OWNER}, lui, n'est pas semé ici mais à chaque démarrage par
 * {@code OwnerSeeder} : il dépend de {@code DISCORD_ADMIN_ID}, une variable d'environnement.
 * Figer une valeur d'environnement dans une migration tracée en base, c'est la rendre impossible
 * à corriger ensuite — exactement le piège que le compose de prod a tendu le 18-09 en portant un
 * id de compte tiers. Le semis à chaque démarrage est de toute façon exigé comme garde-fou
 * anti-verrouillage (plan §1), et il couvre le premier démarrage comme les suivants.</p>
 */
@ChangeUnit(id = "system-roles-seed", order = "003", author = "schub")
public class V003_SystemRolesSeed {

    private static final Logger log = LoggerFactory.getLogger(V003_SystemRolesSeed.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        var roles = mongoTemplate.getCollection("roles");
        for (SystemRole role : SystemRole.values()) {
            if (roles.find(Filters.eq("name", role.roleName())).first() != null) {
                log.info("Rôle système {} déjà présent", role.roleName());
                continue;
            }
            roles.insertOne(new Document()
                    .append("name", role.roleName())
                    .append("permissions", role.permissions().stream().map(Permission::name).toList())
                    .append("system", true)
                    .append("_class", "schultz.thomas.schub.core.data.model.Role"));
            log.info("Rôle système {} créé avec {} permission(s)", role.roleName(), role.permissions().size());
        }
    }

    /**
     * Supprime les quatre rôles système. Les comptes qui les portaient garderaient un
     * {@code roleId} orphelin — c'est sans danger (ils n'auraient plus aucun droit) mais ça
     * n'est réparable que par un nouveau semis, ce que fait le démarrage suivant.
     */
    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        List<String> names = Arrays.stream(SystemRole.values()).map(SystemRole::roleName).toList();
        mongoTemplate.getCollection("roles").deleteMany(Filters.and(
                Filters.in("name", names),
                Filters.eq("system", true)));
    }
}
