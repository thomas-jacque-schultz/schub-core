package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import schultz.thomas.schub.core.team.business.model.GameRole;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "team_champion_pools")
public class TeamChampionPool {

    @Id
    private String teamId;

    private Map<String, List<String>> championKeysByRole = new LinkedHashMap<>();

    private int masteryFloor;

    private Instant updatedAt;

    public List<String> championKeys(GameRole role) {
        if (championKeysByRole == null || role == null) {
            return List.of();
        }
        return championKeysByRole.getOrDefault(role.name(), List.of());
    }

    public void setChampionKeys(GameRole role, List<String> keys) {
        if (championKeysByRole == null) {
            championKeysByRole = new LinkedHashMap<>();
        }
        championKeysByRole.put(role.name(), new ArrayList<>(keys));
    }
}
