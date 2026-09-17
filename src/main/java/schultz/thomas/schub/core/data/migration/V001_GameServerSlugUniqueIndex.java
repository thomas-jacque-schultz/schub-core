package schultz.thomas.schub.core.data.migration;

import schultz.thomas.schub.core.data.model.GameServer;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
/**
 * Pose l'unicité du {@code slug} en base.
 *
 * <p>{@code GameServer.slug} porte {@code @Indexed(unique = true)}, mais cette annotation est
 * <strong>inerte</strong> : Spring Boot 3 laisse {@code auto-index-creation} à {@code false}.
 * L'index n'a donc jamais existé, alors que le slug est l'identité du domaine — clé des URLs,
 * propriétaire des règles de ports, argument des commandes Discord, et lecture chaude via
 * {@code findBySlug}. Deux serveurs de même slug corrompraient la réconciliation des ports.</p>
 *
 * <p>Si la base contient déjà des doublons, cette migration <em>échoue et bloque le
 * démarrage</em>. C'est voulu : le doublon est un défaut de données qu'il faut voir, pas
 * contourner au lancement.</p>
 */
@ChangeUnit(id = "gameserver-slug-unique-index", order = "001", author = "schub")
public class V001_GameServerSlugUniqueIndex {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(GameServer.class)
                .ensureIndex(new Index()
                        .on("slug", Sort.Direction.ASC)
                        .unique()
                        .named("slug_unique"));
    }

    /**
     * Mongock exige une méthode de retour en arrière. Retirer l'index est sans risque pour les
     * données : il ne porte aucune information, seulement une contrainte.
     */
    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(GameServer.class).dropIndex("slug_unique");
    }
}
