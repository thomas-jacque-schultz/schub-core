package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.data.model.User;

// Ce que le compte a de lié : c'est ce que les fronts ouvrent ou verrouillent.
public record AccountLinksDto(boolean discord, boolean riot) {

    public static AccountLinksDto of(User user) {
        return new AccountLinksDto(present(user.getDiscordId()), present(user.getRiotPuuid()));
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
