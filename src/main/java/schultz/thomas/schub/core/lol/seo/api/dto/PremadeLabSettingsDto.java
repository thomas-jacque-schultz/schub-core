package schultz.thomas.schub.core.lol.seo.api.dto;

import schultz.thomas.schub.core.lol.seo.data.model.PremadeLabSettings;

public record PremadeLabSettingsDto(int unknownPlayerBudget, int budgetWindowMinutes) {

    public static PremadeLabSettingsDto of(PremadeLabSettings settings) {
        return new PremadeLabSettingsDto(settings.getUnknownPlayerBudget(), settings.getBudgetWindowMinutes());
    }
}
