package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.Permission;

import java.util.Set;

public record MeDto(
        String userId,
        DiscordIdentityDto discord,
        String displayName,
        boolean displayNameChosen,
        RoleSummaryDto role,
        RiotAccountDto riot,
        AccountLinksDto links,
        SessionSummariesDto sessionSummaries,
        boolean dmUnreachable
) {

    public record SessionSummariesDto(boolean planned, boolean free) {
    }

    public record DiscordIdentityDto(String id, String username, String avatarUrl) {
    }

    public record RoleSummaryDto(String name, Set<Permission> permissions) {
    }
}
