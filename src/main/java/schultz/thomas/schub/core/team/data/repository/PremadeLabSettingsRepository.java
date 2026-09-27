package schultz.thomas.schub.core.team.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.team.data.model.PremadeLabSettings;

public interface PremadeLabSettingsRepository extends MongoRepository<PremadeLabSettings, String> {
}
