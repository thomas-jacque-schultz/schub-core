package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.data.model.FindingRecord;
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
        List<Evaluator.ConditionTrace> evidence,
        Map<String, String> context
) {

    public static FindingDto of(FindingRecord record, PatternVersion pattern) {
        return new FindingDto(record.patternKey(), pattern.getLabel(), pattern.getSentence(), pattern.getPolarity(),
                pattern.getCategory(), pattern.getNature(), record.degree(), pattern.isExperimental(), pattern.getLimits(),
                record.conditions(), record.context());
    }
}
