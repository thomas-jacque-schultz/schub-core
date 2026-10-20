package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PatternDto(
        String key, int version, PatternVersion.Status status, PatternVersion.Scope scope,
        PatternVersion.Polarity polarity, PatternVersion.Category category, PatternVersion.Nature nature,
        List<ConditionDto> required, List<ConditionDto> optional, List<ConditionDto> exceptions,
        double optionalInfluence, double threshold, Map<String, String> label, Map<String, String> sentence,
        boolean experimental, Map<String, String> limits, @Nullable String author, @Nullable String comment, @Nullable Instant createdAt, @Nullable Instant activatedAt) {
}
