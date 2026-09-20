package schultz.thomas.schub.core.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByDiscordId(String discordId);

    Optional<User> findByDiscordUsername(String discordUsername);

    /**
     * Le compte qui a revendiqué ce {@code puuid}.
     *
     * <p>Le {@code puuid} est la seule clé stable côté Riot ; un Riot ID se change. Chercher par
     * pseudo donnerait un jour le compte de quelqu'un d'autre.</p>
     */
    Optional<User> findByRiotPuuid(String riotPuuid);

    List<User> findAllByRoleId(String roleId);

    long countByRoleId(String roleId);
}
