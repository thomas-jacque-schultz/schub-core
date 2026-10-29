package schultz.thomas.schub.core.lol.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Un plan du Macro Planner (Schub#172). elements : l'état du dessin tel que le front le décrit, opaque ici.
// Index posé par V030.
@Data
@Document(collection = "team_macro_plans")
public class MacroPlan {

    @Id
    private String id;

    private String teamId;

    private String name;

    private List<Map<String, Object>> elements = new ArrayList<>();

    private String createdBy;
    private String updatedBy;

    private Instant createdAt;
    private Instant updatedAt;
}
