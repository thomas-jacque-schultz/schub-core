package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;
import java.util.Map;

// Le contenu d'une nouvelle version. Le commentaire est obligatoire : il dit pourquoi la règle change.
public record PatternRequest(
        String key, PatternVersion.Scope scope, PatternVersion.Polarity polarity, PatternVersion.Category category,
        PatternVersion.Nature nature, List<Condition> required, List<Condition> optional,
        List<Condition> exceptions, Double optionalInfluence, Double threshold, Map<String, String> label,
        Map<String, String> sentence, String comment) {
}
