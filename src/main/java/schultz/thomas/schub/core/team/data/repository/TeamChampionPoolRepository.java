package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;

/** L'identifiant du document <strong>est</strong> celui de l'équipe : une équipe, un pool. */
public interface TeamChampionPoolRepository extends MongoRepository<TeamChampionPool, String> {
}
