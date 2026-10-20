package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.data.model.GameRole;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

import jakarta.annotation.Nullable;
import java.util.List;

public record AddMemberRequest(
        String riotGameName,
        String riotTagLine,
        @Nullable String riotPuuid,
        List<GameRole> roles,
        MemberStatus status,
        Boolean coach
) {
}
