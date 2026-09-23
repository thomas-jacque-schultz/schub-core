package schultz.thomas.schub.core.team.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import schultz.thomas.schub.core.team.business.model.GameRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositionSlot {

    private GameRole role;

    private String championId;

    private String memberId;
}
