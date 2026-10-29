package schultz.thomas.schub.core.lol.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.lol.team.data.model.MacroPlan;

import java.util.List;

@Repository
public interface MacroPlanRepository extends MongoRepository<MacroPlan, String> {

    List<MacroPlan> findByTeamIdOrderByUpdatedAtDesc(String teamId);

    void deleteByTeamId(String teamId);
}
