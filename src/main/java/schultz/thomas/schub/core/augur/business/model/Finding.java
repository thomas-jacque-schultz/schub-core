package schultz.thomas.schub.core.augur.business.model;

import schultz.thomas.schub.core.augur.data.model.FindingRecord;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;

// Un constat et la version de règle qui l'a émis.
public record Finding(FindingRecord record, PatternVersion pattern) {
}
