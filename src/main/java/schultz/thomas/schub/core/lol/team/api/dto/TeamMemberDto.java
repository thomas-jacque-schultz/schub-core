package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.data.model.GameRole;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

import java.util.List;

// Ne porte pas l'userId des autres : les droits du lecteur sont servis en booléens par TeamDto.
public record TeamMemberDto(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        List<GameRole> roles,
        MemberStatus status,
        boolean coach,
        boolean linked,
        boolean captain,
        boolean hiddenOnPublicPage
) {
}
