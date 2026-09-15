package schultz.thomas.schub.core.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.model.GameServer;

import java.util.Optional;

@Repository
public interface GameServerRepository extends MongoRepository<GameServer, String> {

    Optional<GameServer> findBySlug(String slug);
}
