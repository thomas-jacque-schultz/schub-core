package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.team.data.model.Team;

import java.util.List;

// Jamais injecté hors de …core.team (interdit n°1 du plan §D.2).
@Repository
public interface TeamRepository extends MongoRepository<Team, String> {

    List<Team> findByMembersUserId(String userId);

    List<Team> findByCreatedBy(String createdBy);
}
