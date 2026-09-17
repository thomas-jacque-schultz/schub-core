package schultz.thomas.schub.core.data.repository;

import schultz.thomas.schub.core.data.model.StaticPortRuleEntity;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StaticPortRuleRepository extends MongoRepository<StaticPortRuleEntity, String> {

    Optional<StaticPortRuleEntity> findByProtoIgnoreCaseAndWanPortStart(String proto, Integer wanPortStart);
}
