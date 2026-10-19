package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.data.model.GameRole;
import schultz.thomas.schub.core.team.data.model.MemberStatus;

import java.util.List;

public record UpdateMemberRequest(List<GameRole> roles, MemberStatus status, Boolean coach) {
}
