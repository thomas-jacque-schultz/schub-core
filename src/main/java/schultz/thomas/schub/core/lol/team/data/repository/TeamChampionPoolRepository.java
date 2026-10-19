package schultz.thomas.schub.core.lol.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.lol.team.data.model.TeamChampionPool;

public interface TeamChampionPoolRepository extends MongoRepository<TeamChampionPool, String> {
}
