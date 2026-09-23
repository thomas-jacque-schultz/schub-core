package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.util.List;

public record UpdateMemberRequest(List<GameRole> roles, MemberStatus status) {
}
