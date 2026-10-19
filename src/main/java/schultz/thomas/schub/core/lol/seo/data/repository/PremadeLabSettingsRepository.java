package schultz.thomas.schub.core.lol.seo.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import schultz.thomas.schub.core.lol.seo.data.model.PremadeLabSettings;

public interface PremadeLabSettingsRepository extends MongoRepository<PremadeLabSettings, String> {
}
