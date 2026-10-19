package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.data.model.PatternVersion;

import java.util.List;
import java.util.Map;

// Un constat tel que le joueur le lit : sans numéro de version, avec sa preuve dépliable.
public record FindingDto(
        String pattern,
        Map<String, String> label,
        Map<String, String> sentence,
        PatternVersion.Polarity polarity,
        PatternVersion.Category category,
        PatternVersion.Nature nature,
        double confidence,
        boolean experimental,
        Map<String, String> limits,
        List<ConditionTraceDto> evidence,
        Map<String, String> context
) {
}
