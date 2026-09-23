package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class TeamMember {

    private String memberId;

    private String userId;

    private String riotPuuid;
    private String riotGameName;
    private String riotTagLine;

    private List<GameRole> roles = new ArrayList<>();

    private MemberStatus status;

    private Instant addedAt;

    private Instant linkedAt;

    public boolean isLinked() {
        return userId != null && !userId.isBlank();
    }

    public boolean playsRole(GameRole role) {
        return role != null && roles != null && roles.contains(role);
    }

    public GameRole mainRole() {
        return roles == null || roles.isEmpty() ? null : roles.getFirst();
    }

    public String riotId() {
        if (riotGameName == null || riotGameName.isBlank()) {
            return null;
        }
        return riotTagLine == null || riotTagLine.isBlank()
                ? riotGameName
                : riotGameName + "#" + riotTagLine;
    }
}
