package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PatternDto(
        String key, int version, PatternVersion.Status status, PatternVersion.Scope scope,
        PatternVersion.Polarity polarity, PatternVersion.Category category, PatternVersion.Nature nature,
        List<Condition> required, List<Condition> optional, List<Condition> exceptions,
        double optionalInfluence, double threshold, Map<String, String> label, Map<String, String> sentence,
        boolean experimental, Map<String, String> limits, String author, String comment, Instant createdAt, Instant activatedAt) {

    public static PatternDto of(PatternVersion p) {
        return new PatternDto(p.getKey(), p.getVersion(), p.getStatus(), p.getScope(), p.getPolarity(),
                p.getCategory(), p.getNature(), p.getRequired(), p.getOptional(), p.getExceptions(),
                p.getOptionalInfluence(), p.getThreshold(), p.getLabel(), p.getSentence(), p.isExperimental(), p.getLimits(), p.getAuthor(),
                p.getComment(), p.getCreatedAt(), p.getActivatedAt());
    }
}
