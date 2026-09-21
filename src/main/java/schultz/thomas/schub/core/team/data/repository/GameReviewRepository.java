package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.team.data.model.GameReview;

import java.util.List;
import java.util.Optional;

/** Comme {@link TeamRepository} : jamais injecté hors du paquet {@code …core.team}. */
@Repository
public interface GameReviewRepository extends MongoRepository<GameReview, String> {

    List<GameReview> findByTeamIdAndMatchIdOrderByCreatedAtAsc(String teamId, String matchId);

    Optional<GameReview> findByMatchIdAndSubjectMemberIdAndAuthorUserId(
            String matchId, String subjectMemberId, String authorUserId);

    void deleteByTeamId(String teamId);

    void deleteByTeamIdAndSubjectMemberId(String teamId, String subjectMemberId);
}
