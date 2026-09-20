package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.team.data.model.Team;

import java.util.List;

/**
 * <strong>Ce dépôt ne s'injecte nulle part hors du paquet {@code …core.team}</strong> — c'est
 * l'interdit n°1 du plan §D.2, et c'est ce qui garde l'extraction du domaine bon marché.
 */
@Repository
public interface TeamRepository extends MongoRepository<Team, String> {

    /**
     * Les équipes où ce compte figure — c'est ce qui fait qu'« une équipe apparaît sur le compte
     * de ses membres ». La requête porte sur l'id interne embarqué dans l'effectif.
     */
    List<Team> findByMembersUserId(String userId);

    List<Team> findByCreatedBy(String createdBy);
}
