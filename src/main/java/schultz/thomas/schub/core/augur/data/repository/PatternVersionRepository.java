package schultz.thomas.schub.core.augur.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;
import java.util.Optional;

public interface PatternVersionRepository extends MongoRepository<PatternVersion, String> {

    List<PatternVersion> findByStatus(PatternVersion.Status status);

    List<PatternVersion> findByKeyOrderByVersionDesc(String key);

    Optional<PatternVersion> findByKeyAndVersion(String key, int version);

    Optional<PatternVersion> findFirstByKeyAndStatus(String key, PatternVersion.Status status);
}
