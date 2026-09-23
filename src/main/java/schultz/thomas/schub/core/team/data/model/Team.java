package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Document(collection = "team_teams")
public class Team {

    @Id
    private String id;

    private String name;

    private String createdBy;

    private List<TeamMember> members = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public Optional<TeamMember> findMember(String memberId) {
        return members == null || memberId == null
                ? Optional.empty()
                : members.stream().filter(member -> memberId.equals(member.getMemberId())).findFirst();
    }

    public boolean hasMemberLinkedTo(String userId) {
        return userId != null && members != null
                && members.stream().anyMatch(member -> userId.equals(member.getUserId()));
    }
}
