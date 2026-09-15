package schultz.thomas.schub.core.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import schultz.thomas.schub.core.model.StaticPortRuleEntity;

import java.util.Optional;

@Repository
public interface StaticPortRuleRepository extends MongoRepository<StaticPortRuleEntity, String> {

    Optional<StaticPortRuleEntity> findByProtoIgnoreCaseAndWanPortStart(String proto, Integer wanPortStart);
}
