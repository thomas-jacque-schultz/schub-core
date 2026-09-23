package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.util.List;

public record AddMemberRequest(
        String riotGameName,
        String riotTagLine,
        String riotPuuid,
        List<GameRole> roles,
        MemberStatus status,
        Boolean coach
) {
}
