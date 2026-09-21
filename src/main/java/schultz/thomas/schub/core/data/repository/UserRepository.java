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

    /**
     * Les comptes qui portent ce Riot ID, casse ignorée.
     *
     * <p><strong>Une liste, pas un {@code Optional}.</strong> Rien ne garantit l'unicité d'un
     * Riot ID en base : c'est une chaîne d'affichage, pas une clé, et elle peut rester périmée
     * sur le compte d'un joueur qui en a changé. Rendre un {@code Optional} promettrait une
     * unicité que seul le {@code puuid} possède — et la promesse casserait sur une exception
     * Mongo le jour où deux comptes en portent le même, ce qui est licite.</p>
     *
     * <p>Ne sert qu'au refus de doublon quand le {@code puuid} n'a pas pu être résolu. Le reste
     * du système ne cherche jamais un joueur par son pseudo.</p>
     */
    List<User> findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(String riotGameName, String riotTagLine);

    /** Les comptes qui ont revendiqué l'un de ces {@code puuid} — une requête pour toute une liste de propositions. */
    List<User> findByRiotPuuidIn(java.util.Collection<String> riotPuuids);

    List<User> findAllByRoleId(String roleId);

    long countByRoleId(String roleId);
}
