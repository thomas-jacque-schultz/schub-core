package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;

/**
 * Pose l'unicité de {@code users.discordId} et de {@code roles.name}.
 *
 * <p>Même raison qu'au V001 : les annotations {@code @Indexed} sont inertes tant que
 * {@code auto-index-creation} est à {@code false}, ce qui est le défaut de Spring Boot 3. Or ces
 * deux contraintes portent de l'identité, pas de la performance — deux comptes pour un même
 * identifiant Discord et la connexion devient non déterministe.</p>
 */
@ChangeUnit(id = "identity-indexes", order = "002", author = "schub")
public class V002_IdentityIndexes {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class)
                .ensureIndex(new Index().on("discordId", Sort.Direction.ASC).unique().named("discordId_unique"));
        mongoTemplate.indexOps(Role.class)
                .ensureIndex(new Index().on("name", Sort.Direction.ASC).unique().named("role_name_unique"));
    }

    /** Retirer un index ne perd aucune donnée : il ne porte qu'une contrainte. */
    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).dropIndex("discordId_unique");
        mongoTemplate.indexOps(Role.class).dropIndex("role_name_unique");
    }
}
