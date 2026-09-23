package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.team.data.model.Composition;

import java.util.List;

@Repository
public interface CompositionRepository extends MongoRepository<Composition, String> {

    List<Composition> findByTeamIdOrderByUpdatedAtDesc(String teamId);

    void deleteByTeamId(String teamId);
}
