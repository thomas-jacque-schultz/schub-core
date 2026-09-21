package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * {@code GameServer.admins} disparaît : un serveur de jeu n'a plus d'administrateur propre.
 *
 * <p>Un administrateur aide sur la machine physique et tient ses droits de son rôle ; un
 * modérateur fait vivre les serveurs de jeu, tous les serveurs de jeu. La liste par serveur
 * donnait une troisième autorité, qu'aucune interface ne rendait ni lisible ni modifiable pour
 * ceux qui en dépendaient.</p>
 *
 * <p>Le champ est retiré des documents plutôt que laissé en place : un champ que plus rien ne lit
 * finit par être relu un jour, et il désignerait alors des droits qui n'existent plus. Le
 * {@code $unset} porte sur les seuls documents qui le portent encore, donc rejouer la migration
 * ne fait rien — ce qui est la propriété qu'on veut d'une migration, Mongock ou pas.</p>
 */
@ChangeUnit(id = "retire-gameserver-admins", order = "009", author = "schub")
public class V009_RetireGameServerAdmins {

    private static final Logger log = LoggerFactory.getLogger(V009_RetireGameServerAdmins.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        UpdateResult result = mongoTemplate.getCollection("servers").updateMany(
                Filters.exists("admins"),
                Updates.unset("admins"));
        log.info("Administrateurs par serveur retirés de {} document(s)", result.getModifiedCount());
    }

    /**
     * Rien à rétablir : les listes retirées désignaient une autorité qui n'existe plus dans le
     * code. Les remettre rendrait des documents que plus aucune règle ne lit.
     */
    @RollbackExecution
    public void rollback() {
        log.warn("Retour en arrière sans objet : l'autorité par serveur n'existe plus");
    }
}
