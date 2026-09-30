package schultz.thomas.schub.core.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Une équipe sans pool reçoit la sélection de départ ; un pool déjà écrit, même vide, n'est pas touché.
@ChangeUnit(id = "starting-champion-pool", order = "024", author = "schub")
public class V024_StartingChampionPool {

    private static final Logger log = LoggerFactory.getLogger(V024_StartingChampionPool.class);

    @Execution
    public void execution(MongoTemplate mongo) {
        Set<String> avecPool = new HashSet<>();
        mongo.findAll(TeamChampionPool.class).forEach(pool -> avecPool.add(pool.getTeamId()));
        List<String> sansPool = mongo.findAll(Team.class).stream()
                .map(Team::getId)
                .filter(id -> !avecPool.contains(id))
                .toList();
        sansPool.forEach(id -> mongo.insert(TeamChampionPool.depart(id)));
        log.info("{} équipe(s) sans pool : sélection de départ posée", sansPool.size());
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.remove(Query.query(Criteria.where("startingSelection").is(true)), TeamChampionPool.class);
    }
}
