package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import schultz.thomas.schub.core.data.model.User;

/**
 * Un {@code puuid} appartient à un compte, et à un seul.
 *
 * <h2>Pourquoi un index alors que le service refuse déjà les doublons</h2>
 *
 * <p>Le refus applicatif de {@code RiotAccountService} est une lecture suivie d'une écriture :
 * entre les deux, une seconde requête peut passer. À cinq joueurs la fenêtre est théorique, mais
 * le dégât ne l'est pas — deux comptes portant le même {@code puuid} donnent des statistiques
 * attribuées à la mauvaise personne, sans que rien ne le signale, et tout le chantier D part de
 * cette clé. L'index est ce qui rend la règle vraie plutôt que probable.</p>
 *
 * <p>Il sert aussi de chemin de lecture : {@code findByRiotPuuid} est appelée à chaque ajout de
 * membre et à chaque revendication, et sans index c'est un balayage complet des comptes.</p>
 *
 * <h2>Partiel, et pas seulement {@code sparse}</h2>
 *
 * <p>La très grande majorité des comptes n'a pas de {@code puuid}. Un index unique ordinaire les
 * ferait tous entrer en collision sur {@code null} dès le deuxième compte créé. {@code sparse}
 * suffirait tant que Spring Data omet les champs nuls à l'écriture — ce qu'il fait aujourd'hui,
 * par défaut, et qui n'est pas une garantie sur laquelle on veut faire reposer la création de
 * comptes. Le filtre partiel, lui, exclut explicitement tout ce qui n'est pas une chaîne :
 * absent, nul ou vide, c'est hors de l'index.</p>
 *
 * <h2>Si elle échoue</h2>
 *
 * <p>Elle échoue si deux comptes portent déjà le même {@code puuid}, et le démarrage s'arrête là.
 * C'est voulu : ces deux comptes sont une donnée fausse, et il faut décider lequel a raison. La
 * prod est vierge au moment où cette migration est écrite, donc le cas est théorique — mais
 * découvrir le doublon au démarrage vaut mieux que le découvrir dans un tableau de statistiques.</p>
 */
@ChangeUnit(id = "riot-puuid-unique-index", order = "007", author = "schub")
public class V007_RiotPuuidUniqueIndex {

    private static final String NOM = "riotPuuid_unique";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).ensureIndex(
                new Index()
                        .on("riotPuuid", Sort.Direction.ASC)
                        .unique()
                        .named(NOM)
                        .partial(PartialIndexFilter.of(
                                new Document("riotPuuid", new Document("$type", "string")))));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(User.class).dropIndex(NOM);
    }
}
