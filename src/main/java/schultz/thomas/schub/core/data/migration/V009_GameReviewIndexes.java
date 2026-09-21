package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import schultz.thomas.schub.core.team.data.model.GameReview;

/**
 * Une note de revue par auteur, par joueur et par partie.
 *
 * <p>Même raison qu'au V007 : {@code @CompoundIndex} est inerte tant que
 * {@code auto-index-creation} est à {@code false}, et le refus applicatif de
 * {@code GameReviewService} est une lecture suivie d'une écriture — deux envois simultanés
 * passeraient tous les deux. L'index est ce qui rend la règle vraie plutôt que probable.</p>
 *
 * <p>Elle importe l'entité du paquet {@code team} : une migration d'index connaît le schéma
 * physique par nature, et le nom de collection écrit en dur divergerait un jour en silence. Ce
 * n'est pas le domaine hébergement qui lit {@code team}, c'est l'amorçage de la base.</p>
 *
 * <p>Le second index sert le chemin de lecture : ouvrir le débrief d'une partie demande les notes
 * de cette équipe pour ce {@code matchId}, et sans lui c'est un balayage de la collection.</p>
 */
@ChangeUnit(id = "game-review-indexes", order = "009", author = "schub")
public class V009_GameReviewIndexes {

    private static final String UNIQUE = "revue_unique";
    private static final String PAR_PARTIE = "revue_equipe_partie";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(GameReview.class).ensureIndex(
                new CompoundIndexDefinition(new Document()
                        .append("matchId", 1)
                        .append("subjectMemberId", 1)
                        .append("authorUserId", 1))
                        .unique()
                        .named(UNIQUE));

        mongoTemplate.indexOps(GameReview.class).ensureIndex(
                new CompoundIndexDefinition(new Document()
                        .append("teamId", 1)
                        .append("matchId", 1)
                        .append("createdAt", 1))
                        .named(PAR_PARTIE));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(GameReview.class).dropIndex(UNIQUE);
        mongoTemplate.indexOps(GameReview.class).dropIndex(PAR_PARTIE);
    }
}
