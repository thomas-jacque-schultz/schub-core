package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.data.model.PremadeLabSettings;

public record PremadeLabSettingsDto(int unknownPlayerBudget, int budgetWindowMinutes) {

    public static PremadeLabSettingsDto of(PremadeLabSettings settings) {
        return new PremadeLabSettingsDto(settings.getUnknownPlayerBudget(), settings.getBudgetWindowMinutes());
    }
}
