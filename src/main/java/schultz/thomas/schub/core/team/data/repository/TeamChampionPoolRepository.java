package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;

public interface TeamChampionPoolRepository extends MongoRepository<TeamChampionPool, String> {
}
