package schultz.thomas.schub.core.augur.data.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.augur.data.model.FindingRecord;

import java.util.Collection;
import java.util.List;

public interface FindingRecordRepository extends MongoRepository<FindingRecord, String> {

    List<FindingRecord> findBySubjectIn(Collection<String> subjects);

    List<FindingRecord> findByPatternKeyAndStaleTrue(String patternKey, Pageable page);

    List<FindingRecord> findByScopeOrderByComputedAtDesc(schultz.thomas.schub.core.augur.data.model.PatternVersion.Scope scope,
                                                        Pageable page);
}
