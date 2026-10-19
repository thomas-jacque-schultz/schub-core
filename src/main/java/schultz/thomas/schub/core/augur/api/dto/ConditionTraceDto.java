package schultz.thomas.schub.core.augur.api.dto;

import schultz.thomas.schub.core.augur.data.model.Condition;
import schultz.thomas.schub.core.augur.data.model.ConditionTrace;

import jakarta.annotation.Nullable;

public record ConditionTraceDto(ConditionTrace.Role role, String signal, Condition.Unit unit, double from, double to,
                                @Nullable Double observed, @Nullable Double value, @Nullable Double degree) {
}
