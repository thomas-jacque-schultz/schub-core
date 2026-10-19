package schultz.thomas.schub.core.augur.api.dto;

import java.util.List;

public record EvaluationDto(String patternKey, int version, double degree, boolean emitted, boolean excepted,
                            List<ConditionTraceDto> conditions) {
}
