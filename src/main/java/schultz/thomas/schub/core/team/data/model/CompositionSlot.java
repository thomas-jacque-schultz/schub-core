package schultz.thomas.schub.core.team.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import schultz.thomas.schub.core.team.business.model.GameRole;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositionSlot {

    private GameRole role;

    private String championId;

    private String memberId;

    private List<String> alternatives = new ArrayList<>();

    public CompositionSlot(GameRole role, String championId, String memberId) {
        this(role, championId, memberId, new ArrayList<>());
    }
}
