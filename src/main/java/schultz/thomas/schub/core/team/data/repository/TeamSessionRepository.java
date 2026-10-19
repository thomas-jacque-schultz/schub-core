package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.team.data.model.TeamSession;

import java.time.LocalDate;
import java.util.List;

public interface TeamSessionRepository extends MongoRepository<TeamSession, String> {

    List<TeamSession> findByTeamIdAndDayGreaterThanEqualOrderByStartedAtDesc(String teamId, LocalDate depuis);

    List<TeamSession> findByDay(LocalDate day);

    List<TeamSession> findByDayGreaterThanEqualOrderByStartedAtDesc(LocalDate depuis);
}
