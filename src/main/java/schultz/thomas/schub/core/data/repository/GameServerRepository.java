package schultz.thomas.schub.core.data.repository;

import schultz.thomas.schub.core.data.model.GameServer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GameServerRepository extends MongoRepository<GameServer, String> {

    Optional<GameServer> findBySlug(String slug);
}
