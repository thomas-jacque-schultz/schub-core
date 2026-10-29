package schultz.thomas.schub.core.lol.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.lol.team.data.model.Team;

import java.util.List;

// Jamais injecté hors de …core.lol (interdit n°1 du plan §D.2).
@Repository
public interface TeamRepository extends MongoRepository<Team, String> {

    List<Team> findByMembersUserId(String userId);

    List<Team> findByPublicPageTrue();

    List<Team> findByCreatedBy(String createdBy);

    List<Team> findByMembersRiotPuuidIn(java.util.Collection<String> riotPuuids);

    @org.springframework.data.mongodb.repository.Query(value = "{ 'members.riotPuuid': { $ne: null } }", fields = "{ 'members.riotPuuid': 1 }")
    List<Team> findRiotPuuids();

    @org.springframework.data.mongodb.repository.Query("{ 'members': { $elemMatch: { $or: [ { 'riotPuuid': null }, { 'riotPuuid': '' } ] } } }")
    List<Team> findWithUnresolvedMembers();

    @org.springframework.data.mongodb.repository.Query("{ 'members': { $elemMatch: { 'userId': null, 'riotPuuid': { $nin: [null, ''] } } } }")
    List<Team> findWithUnlinkedResolvedMembers();
}
