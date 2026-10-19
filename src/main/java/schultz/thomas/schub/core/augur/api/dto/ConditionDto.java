package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.data.model.Condition;

public record ConditionDto(String signal, Condition.Unit unit, double from, double to, double weight) {
}
